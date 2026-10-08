package com.imtrainer.round.service;

import com.imtrainer.round.exception.RoundAlreadyInProgressException;
import com.imtrainer.round.exception.RoundNotFoundException;
import com.imtrainer.round.validation.RoundValidator;
import com.imtrainer.round.dto.RoundEndRequest;
import com.imtrainer.round.exception.RoundAlreadyInProgressException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RoundService {

    private static final String KEY_PREFIX = "round:";
    // private static final Duration ROUND_TTL = Duration.ofMinutes(1); // 일단 테스트를 위해 1분으로 잡았습니다. 추후에 수정 필요
    private static final Duration ROUND_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final RoundValidator roundValidator;
    private final LeaderboardService leaderboardService;

    public void startRound(String trainerId) {
        String key = KEY_PREFIX + trainerId;
        String startedAt = String.valueOf(Instant.now().toEpochMilli());

        Boolean saved = redisTemplate.opsForValue().setIfAbsent(key, startedAt, ROUND_TTL);
        if (Boolean.FALSE.equals(saved)) {
            throw new RoundAlreadyInProgressException();
        }
    }

    public void endRound(RoundEndRequest request) {
        // 꺼내면서 동시에 삭제 -> 검증 실패해도 라운드 종료, 재전송 방지 (Redis 6.2+)
        String startedAt = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + request.trainerId());
        if (startedAt == null) {
            throw new RoundNotFoundException();
        }

        double serverElapsedSec = (Instant.now().toEpochMilli() - Long.parseLong(startedAt)) / 1000.0;
        roundValidator.validate(request, serverElapsedSec);

        leaderboardService.record(request);
    }
}
