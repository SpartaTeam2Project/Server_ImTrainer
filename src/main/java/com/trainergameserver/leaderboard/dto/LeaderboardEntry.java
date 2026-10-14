package com.trainergameserver.leaderboard.dto;

import com.trainergameserver.leaderboard.entity.LeaderboardRecord;
import com.trainergameserver.round.dto.RoundResult;

import java.time.LocalDateTime;

public record LeaderboardEntry(
        int rank,
        String trainerId,
        int killCount,
        double gameTime,
        RoundResult result,
        LocalDateTime recordedAt
) {
    public static LeaderboardEntry of(int rank, LeaderboardRecord r) {
        return new LeaderboardEntry(rank, r.getTrainerId(), r.getKillCount(),
                r.getGameTime(), r.getResult(), r.getRecordedAt());
    }
}
