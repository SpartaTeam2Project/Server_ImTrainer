package com.trainergameserver.auth.security;

import com.trainergameserver.common.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 실패 횟수 제한 (비밀번호 대입 공격 방지).
 * <ul>
 *   <li>같은 IP + 아이디: max-failures-per-account번 실패하면 block-seconds 동안 차단</li>
 *   <li>같은 IP 전체: max-failures-per-ip번 실패하면 block-seconds 동안 차단 (아이디를 바꿔 가며 시도하는 경우)</li>
 * </ul>
 * 아이디만 기준으로 막으면 남의 아이디를 일부러 잠글 수 있으므로 IP와 함께 묶습니다.
 * 카운트는 서버 메모리에 있어 서버를 재시작하면 초기화되고, 서버가 여러 대면 서버별로 셉니다.
 */
@Component
public class LoginAttemptLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptLimiter.class);

    private final int maxFailuresPerAccount;
    private final int maxFailuresPerIp;
    private final long blockMillis;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(@Value("${login.max-failures-per-account}") int maxFailuresPerAccount,
                               @Value("${login.max-failures-per-ip}") int maxFailuresPerIp,
                               @Value("${login.block-seconds}") long blockSeconds) {
        this.maxFailuresPerAccount = maxFailuresPerAccount;
        this.maxFailuresPerIp = maxFailuresPerIp;
        this.blockMillis = blockSeconds * 1000;
    }

    /** 차단 중이면 TooManyRequestsException. 비밀번호 비교 전에 호출합니다. */
    public void checkAllowed(String ip, String username) {
        long now = System.currentTimeMillis();
        long retryAfterMillis = Math.max(
                remainingBlockMillis(accountKey(ip, username), now),
                remainingBlockMillis(ipKey(ip), now));
        if (retryAfterMillis > 0) {
            long retryAfterSeconds = (retryAfterMillis + 999) / 1000;
            long minutes = (retryAfterSeconds + 59) / 60;
            throw new TooManyRequestsException("login", retryAfterSeconds, minutes);
        }
    }

    public void recordFailure(String ip, String username) {
        long now = System.currentTimeMillis();
        int accountFailures = increment(accountKey(ip, username), maxFailuresPerAccount, now);
        int ipFailures = increment(ipKey(ip), maxFailuresPerIp, now);
        if (accountFailures == maxFailuresPerAccount || ipFailures == maxFailuresPerIp) {
            log.warn("로그인 시도 제한 걸림 ip={} username={} (계정 {}회, IP {}회 실패)",
                    ip, username, accountFailures, ipFailures);
        }
    }

    /** 로그인에 성공하면 그 IP + 아이디의 실패 기록만 지웁니다. (IP 전체 기록은 유지) */
    public void recordSuccess(String ip, String username) {
        windows.remove(accountKey(ip, username));
    }

    /** 차단 시간이 지난 기록을 정리해 메모리가 계속 늘지 않게 합니다. */
    @Scheduled(fixedDelay = 600_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        windows.values().removeIf(window -> window.isExpired(now, blockMillis));
    }

    /** 테스트용: 모든 기록 삭제 */
    public void clear() {
        windows.clear();
    }

    private long remainingBlockMillis(String key, long now) {
        Window window = windows.get(key);
        if (window == null || window.blockedUntil() <= now) {
            return 0;
        }
        return window.blockedUntil() - now;
    }

    private int increment(String key, int maxFailures, long now) {
        return windows.compute(key, (k, window) -> {
            Window next = (window == null || window.isExpired(now, blockMillis))
                    ? new Window(now, 1, 0)
                    : window.plusOne();
            // 한도에 도달한 시점부터 blockMillis 동안 차단
            return (next.failures() >= maxFailures && next.blockedUntil() == 0)
                    ? next.blockUntil(now + blockMillis)
                    : next;
        }).failures();
    }

    private static String accountKey(String ip, String username) {
        // DB 정렬 규칙상 아이디는 대소문자를 구분하지 않으므로 같게 취급
        return "account|" + ip + "|" + username.toLowerCase(Locale.ROOT);
    }

    private static String ipKey(String ip) {
        return "ip|" + ip;
    }

    /**
     * 실패 기록. startedAt부터 blockMillis 안의 실패를 세고, 한도에 도달하면 blockedUntil까지 차단합니다.
     * blockedUntil이 0이면 아직 차단 전입니다.
     */
    private record Window(long startedAt, int failures, long blockedUntil) {

        Window plusOne() {
            return new Window(startedAt, failures + 1, blockedUntil);
        }

        Window blockUntil(long until) {
            return new Window(startedAt, failures, until);
        }

        boolean isExpired(long now, long blockMillis) {
            return blockedUntil == 0 ? now - startedAt >= blockMillis : now >= blockedUntil;
        }
    }
}
