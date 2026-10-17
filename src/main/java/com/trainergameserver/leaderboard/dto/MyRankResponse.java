package com.trainergameserver.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 순위 응답.
 * Unity JsonUtility 는 null 객체를 빈 객체(값 0)로 채워서 "기록 없음"과 구분이 안 되므로 ranked 로 따로 알려준다.
 */
@JsonPropertyOrder({"ranked", "entry"})
public record MyRankResponse(
        @Schema(description = "리더보드 기록이 있는지. false 면 entry 는 null", example = "true") boolean ranked,
        @Schema(description = "내 순위와 최고 기록. 상위 목록 밖이어도 몇 등인지 나옴") LeaderboardEntry entry) {

    public static MyRankResponse of(LeaderboardEntry entry) {
        return new MyRankResponse(entry != null, entry);
    }
}
