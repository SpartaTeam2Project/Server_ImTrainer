package com.trainergameserver.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 플레이데이터 검증 실패(409) 응답. ErrorResponse에 서버 기준 playerData가 추가된 형태입니다. */
@Schema(description = "플레이데이터 검증 실패 응답. 클라는 playerData로 덮어씁니다.")
public record PlayerErrorResponse(
        @Schema(description = "에러 코드 (MISMATCH, NOT_ENOUGH_CURRENCY, REQUIREMENT_NOT_MET, MAX_LEVEL, "
                + "NOT_OWNED, INTRO_ALREADY_DONE, UNKNOWN_ID)") String code,
        @Schema(description = "사용자에게 보여 줄 수 있는 에러 메시지") String message,
        @Schema(description = "서버 기준 플레이데이터") PlayerData playerData) {
}
