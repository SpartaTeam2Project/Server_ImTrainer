package com.trainergameserver.round.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.trainergameserver.player.dto.PlayerData;
import io.swagger.v3.oas.annotations.media.Schema;

/** 라운드 종료 성공 응답. 클라는 playerData로 플레이데이터를 덮어씁니다. */
@Schema(description = "라운드 종료 결과")
@JsonPropertyOrder({"reward", "playerData"})
public record RoundEndResponse(
        @Schema(description = "이번 라운드로 지급된 pocket_dollar. 주운 포켓달러 × (1 + ShardBonus), 0.5 이상 올림. CLEAR/DEAD 모두", example = "550")
        long reward,
        @Schema(description = "지급 후 플레이데이터 (서버 기준 전체 값)")
        PlayerData playerData) {
}
