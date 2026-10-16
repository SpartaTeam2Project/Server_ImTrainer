package com.trainergameserver.round.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trainergameserver.player.dto.PlayerData;
import io.swagger.v3.oas.annotations.media.Schema;

/** 라운드 종료 성공 응답. 클라는 playerData로 플레이데이터를 덮어씁니다. */
@Schema(description = "라운드 종료 결과")
@JsonPropertyOrder({"reward", "playerData"})
public record RoundEndResponse(
        @Schema(description = "이번 라운드로 지급된 pocket_dollar. CLEAR면 남은 몬스터볼(current), DEAD면 0", example = "500")
        long reward,
        @Schema(description = "지급 후 플레이데이터 (서버 기준 전체 값)")
        PlayerData playerData) {
}
