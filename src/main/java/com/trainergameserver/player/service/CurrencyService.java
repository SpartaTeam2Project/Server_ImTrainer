package com.trainergameserver.player.service;

import com.trainergameserver.auth.entity.User;
import com.trainergameserver.auth.repository.UserRepository;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.player.entity.Player;
import com.trainergameserver.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재화(스테이지가 끝나도 남는 재화) 지급·차감. 재화를 바꾸는 코드는 모두 이 서비스를 거칩니다.
 * <p>
 * <b>호출하는 쪽에서 트랜잭션을 열어야 합니다</b> (Propagation.MANDATORY, 없으면 IllegalTransactionStateException).
 * 재화 변경과 그 원인(스테이지 결과 저장, 트레이닝 레벨 변경 등)을 한 트랜잭션으로 묶어,
 * 한쪽만 저장되는 일이 없게 하기 위해서입니다.
 * <p>
 * 잠금은 트레이닝 구매와 같은 players 행 잠금(SELECT ... FOR UPDATE)을 씁니다. 같은 계정의 구매·지급이
 * 동시에 와도 하나씩 처리됩니다. 같은 트랜잭션에서 이미 잠근 행을 다시 잠가도 문제없습니다.
 *
 * <pre>
 * // 예: 스테이지 결과 저장 (userId는 컨트롤러의 @AuthUser username → UserRepository로 조회)
 * &#64;Transactional
 * public StageResultResponse saveResult(Long userId, StageResultRequest request) {
 *     ... 결과 저장, runId 중복 확인 ...
 *     long balance = currencyService.grant(userId, CurrencyService.POCKET_DOLLAR, deposited);
 * }
 * </pre>
 */
@Service
public class CurrencyService {

    /** 지금 있는 유일한 재화 */
    public static final String POCKET_DOLLAR = "pocket_dollar";

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;

    public CurrencyService(PlayerRepository playerRepository, UserRepository userRepository) {
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
    }

    /** 잔액에 amount를 더하고 변경 후 잔액을 돌려줍니다. 플레이데이터가 없는 계정이면 먼저 만듭니다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long grant(Long userId, String currencyId, long amount) {
        requireNotNegative(amount);
        Player player = lockPlayer(userId);
        long after = Math.addExact(player.balance(currencyId), amount);
        player.setBalance(currencyId, after);
        return after;
    }

    /**
     * 잔액에서 amount를 빼고 변경 후 잔액을 돌려줍니다.
     * 잔액이 부족하면 409 NOT_ENOUGH_CURRENCY (서버 기준 playerData 포함)를 던지고, 트랜잭션은 롤백됩니다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long spend(Long userId, String currencyId, long amount) {
        requireNotNegative(amount);
        Player player = lockPlayer(userId);
        long balance = player.balance(currencyId);
        if (balance < amount) {
            throw new PlayerConflictException(ErrorCode.NOT_ENOUGH_CURRENCY, null, PlayerService.toData(player));
        }
        long after = balance - amount;
        player.setBalance(currencyId, after);
        return after;
    }

    /** 현재 잔액 (잠그지 않음) */
    @Transactional(readOnly = true)
    public long balance(Long userId, String currencyId) {
        return playerRepository.findById(userId).map(player -> player.balance(currencyId)).orElse(0L);
    }

    private Player lockPlayer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("없는 계정입니다. userId=" + userId));
        playerRepository.createIfAbsent(user.getId(), user.getUsername());
        return playerRepository.findForUpdate(user.getId()).orElseThrow();
    }

    private static void requireNotNegative(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount는 0 이상이어야 합니다: " + amount);
        }
    }
}
