package com.trainergameserver.leaderboard.entity;

import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 트레이너별 최고 기록 1건 (테이블은 db/migration/V5__leaderboard.sql).
 * trainerId = users.username
 * 랭킹 기준: 처치 수 많은 순, 같으면 먼저 달성한 순. 바꾸면 updateIfBetter 와 Repository 정렬을 함께 바꾼다.
 */
@Entity
@Table(name = "leaderboard")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaderboardRecord {

    @Id
    @Column(length = 20)
    private String trainerId;

    @Column(nullable = false)
    private Integer killCount;

    // ddl-auto=validate: Hibernate 기본 기대 타입(float(53))과 DOUBLE 이 다르게 보일 수 있어 명시
    @Column(nullable = false, columnDefinition = "DOUBLE")
    private Double gameTime;

    // ddl-auto=validate: MySQL 에서 Hibernate 가 enum 컬럼을 기대할 수 있어 VARCHAR 로 명시
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10)")
    private RoundResult result;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    public static LeaderboardRecord of(String trainerId, RoundEndRequest req, LocalDateTime now) {
        LeaderboardRecord r = new LeaderboardRecord();
        r.trainerId = trainerId;
        r.apply(req, now);
        return r;
    }

    /** 새 기록이 더 좋을 때만 갱신 */
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
