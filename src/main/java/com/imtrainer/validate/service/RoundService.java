package com.imtrainer.validate.service;

import com.imtrainer.validate.exception.InvalidTimeException;
import com.imtrainer.validate.exception.RoundAlreadyInProgressException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoundService {

    private static final String KEY_PREFIX = "round:";
    private static final Duration ROUND_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public void startRound(String trainerId) {
        String key = KEY_PREFIX + trainerId;
        String startedAt = String.valueOf(Instant.now().toEpochMilli());

        Boolean saved = redisTemplate.opsForValue().setIfAbsent(key, startedAt, ROUND_TTL);
        if (Boolean.FALSE.equals(saved)) {
            throw new RoundAlreadyInProgressException();
        }
    }
}
