package com.imtrainer.round.service;

import com.imtrainer.round.dto.RoundEndRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeaderboardService {
    private static final String KEY = "leaderboard";

    private final StringRedisTemplate redisTemplate;
    public void record(RoundEndRequest req) {
        ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();
        double score = calculateScore(req);

        Double best = zSet.score(KEY, req.trainerId());
        if (best == null || score > best) {
            zSet.add(KEY, req.trainerId(), score);
        }
    }
    private double calculateScore(RoundEndRequest req) {
        return req.killCount();
    }

}
