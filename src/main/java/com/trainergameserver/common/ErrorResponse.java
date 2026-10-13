package com.trainergameserver.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드 (INVALID_REQUEST, UNAUTHORIZED, DUPLICATE_USERNAME)") String code,
        @Schema(description = "사용자에게 보여 줄 수 있는 에러 메시지") String message) {
}
