package com.imtrainer.validate.service;

import com.imtrainer.validate.exception.InvalidTimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoundService {

    private static final Duration ALLOWED_FUTURE = Duration.ofSeconds(5);
    private static final Duration ALLOWED_PAST = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public void startRound(String trainerId, LocalDateTime startTime) {
        validateStartTime(startTime);
        String key = "round:" + trainerId;
        redisTemplate.opsForValue().set(key, startTime.toString(), Duration.ofSeconds(1800));
    }

    private void validateStartTime(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isAfter(now.plus(ALLOWED_FUTURE))
                || startTime.isBefore(now.minus(ALLOWED_PAST))) {
            throw new InvalidTimeException();
        }
    }
}
