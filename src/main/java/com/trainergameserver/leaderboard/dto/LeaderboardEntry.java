package com.trainergameserver.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trainergameserver.round.dto.RoundResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 랭킹 한 줄. 로그인 아이디는 내려주지 않고 플레이데이터의 닉네임을 씁니다. */
@JsonPropertyOrder({"rank", "nickname", "killCount", "gameTime", "result", "recordedAt"})
public record LeaderboardEntry(
        @Schema(example = "1") int rank,
        @Schema(description = "닉네임 (인트로에서 정한 이름)", example = "레드") String nickname,
        @Schema(example = "1820") int killCount,
        @Schema(description = "게임 내 경과 시간(초)", example = "1800.5") double gameTime,
        RoundResult result,
        @Schema(description = "기록 달성 시각", example = "2026-10-15T14:30:00") LocalDateTime recordedAt) {
}
