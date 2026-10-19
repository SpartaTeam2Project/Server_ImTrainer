package com.trainergameserver.leaderboard.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 랭킹 한 줄. 로그인 아이디는 내려주지 않고 플레이데이터의 닉네임을 씁니다. */
@JsonPropertyOrder({"rank", "nickname", "killCount", "pokemonEntry"})
public record LeaderboardEntry(
        @Schema(description = "순위", example = "1") int rank,
        @Schema(description = "닉네임 (인트로에서 정한 이름)", example = "레드") String nickname,
        @Schema(description = "최고 기록을 세운 판(클리어)의 최종 레벨", example = "36") int level,

        @Schema(description = "처치 수", example = "1820") int killCount,
        @Schema(description = "최고 기록을 세운 판의 장착 포켓몬 도감번호", example = "[25, 133]") List<Integer> pokemonEntry) {
}
