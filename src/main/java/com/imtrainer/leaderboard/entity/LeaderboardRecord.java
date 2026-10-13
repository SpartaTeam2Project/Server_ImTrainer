package com.imtrainer.leaderboard.entity;


import com.imtrainer.round.dto.RoundEndRequest;
import com.imtrainer.round.dto.RoundResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard", indexes = @Index(name = "idx_leaderboard_rank", columnList = "kill_count, recorded_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaderboardRecord {
    @Id
    @Column(length = 20)
    private String trainerId;
    @Column(nullable = false)
    private Integer killCount;

    @Column(nullable = false)
    private Double gameTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoundResult result;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    public static LeaderboardRecord from(RoundEndRequest req, LocalDateTime now) {
        LeaderboardRecord r = new LeaderboardRecord();
        r.trainerId = req.trainerId();
        r.apply(req, now);
        return r;
    }

    /** 새 기록이 더 좋을 때만 갱신. 랭킹 기준을 바꾸면 여기와 Repository 정렬을 함께 바꾼다 */
    public void updateIfBetter(RoundEndRequest req, LocalDateTime now) {
        if (req.killCount() > this.killCount) {
            apply(req, now);
        }
    }

    private void apply(RoundEndRequest req, LocalDateTime now) {
        this.killCount = req.killCount();
        this.gameTime = req.gameTime();
        this.result = req.result();
        this.recordedAt = now;
    }
}
