package com.trainergameserver.leaderboard.entity;

import com.trainergameserver.round.dto.RoundEndRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 트레이너별 최고 기록 1건 (테이블은 db/migration/V5__leaderboard.sql).
 * 랭킹 응답에 쓰는 처치 수, 장착 포켓몬과 동점 판정용 달성 시각만 저장합니다.
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

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    /** 최고 기록을 세운 판의 장착 포켓몬 도감번호. "25,133" 형태로 저장 */
    @Column(nullable = false, length = 50)
    private String pokemonEntry;

    /** 최고 기록을 세운 판(클리어)의 최종 레벨 (V7) */
    @Column(nullable = false)
    private Integer level;

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
        this.recordedAt = now;
        this.level = req.level();
        this.pokemonEntry = req.pokemonEntry().stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /** 저장된 엔트리를 도감번호 목록으로 */
    public List<Integer> pokemonEntryIds() {
        if (pokemonEntry == null || pokemonEntry.isBlank()) {
            return List.of();
        }
        return Arrays.stream(pokemonEntry.split(",")).map(Integer::valueOf).toList();
    }
}
