package com.trainergameserver.round.service;

import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.leaderboard.service.LeaderboardService;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.validation.RoundValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 진행 중인 라운드의 시작 시각은 Redis(round:{username})에 둡니다.
 * 종료 요청 때 꺼내면서 삭제하므로, 같은 라운드 결과를 두 번 제출할 수 없습니다.
 */
@Service
public class RoundService {

    private static final Logger log = LoggerFactory.getLogger(RoundService.class);

    private static final String KEY_PREFIX = "round:";
    /** 종료 요청이 오지 않은 라운드(튕김, 강제 종료)를 치우는 청소용 시간. 정상 플레이(30분 + 일시정지)보다 넉넉하게 */
    private static final Duration ROUND_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final RoundValidator roundValidator;
    private final LeaderboardService leaderboardService;

    public RoundService(StringRedisTemplate redisTemplate, RoundValidator roundValidator,
                        LeaderboardService leaderboardService) {
        this.redisTemplate = redisTemplate;
        this.roundValidator = roundValidator;
        this.leaderboardService = leaderboardService;
    }

    /** 진행 중인 라운드가 있으면 포기로 보고 덮어쓴다 (새 런 시작 = 이전 런 종료) */
    public void startRound(String username) {
        String key = KEY_PREFIX + username;
        String startedAt = String.valueOf(Instant.now().toEpochMilli());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            log.info("이전 라운드 포기 처리: {}", username);
        }
        redisTemplate.opsForValue().set(key, startedAt, ROUND_TTL);
    }

    public void endRound(String username, RoundEndRequest request) {
        // 꺼내면서 삭제 (Redis 6.2+). 검증에 실패해도 라운드는 종료된다
        String startedAt = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + username);
        if (startedAt == null) {
            throw new ApiException(ErrorCode.ROUND_NOT_FOUND);
        }

        double serverElapsedSec = (Instant.now().toEpochMilli() - Long.parseLong(startedAt)) / 1000.0;
        roundValidator.validate(request, serverElapsedSec);

        leaderboardService.record(username, request, LocalDateTime.now());
        // TODO 재화 지급: 지급량(deposited) 계산 규칙이 정해지면 @Transactional 을 붙이고
        //      currencyService.grant(userId, CurrencyService.POCKET_DOLLAR, deposited) 호출
    }
}
