package com.imtrainer.round.service;

import com.imtrainer.leaderboard.service.LeaderboardService;
import com.imtrainer.round.dto.RoundEndRequest;
import com.imtrainer.round.exception.RoundNotFoundException;
import com.imtrainer.round.validation.RoundValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoundService {

    private static final Logger log = LoggerFactory.getLogger(RoundService.class);
    private static final String KEY_PREFIX = "round:";
    private static final Duration ROUND_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final RoundValidator roundValidator;
    private final LeaderboardService leaderboardService;

    public void startRound(String token) {
        String key = KEY_PREFIX + token;
        String startedAt = String.valueOf(Instant.now().toEpochMilli());

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            log.info("이전 라운드 포기 처리: {}", token);
        }
        redisTemplate.opsForValue().set(key, startedAt, ROUND_TTL);
    }

    public void endRound(RoundEndRequest request) {
        String startedAt = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + request.trainerId());
        if (startedAt == null) {
            throw new RoundNotFoundException();
        }

        double serverElapsedSec = (Instant.now().toEpochMilli() - Long.parseLong(startedAt)) / 1000.0;
        roundValidator.validate(request, serverElapsedSec);

        leaderboardService.record(request, LocalDateTime.now());
    }
}
