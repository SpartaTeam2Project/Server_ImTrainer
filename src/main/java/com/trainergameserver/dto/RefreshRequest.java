package com.trainergameserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "리프레시 토큰 요청 (refresh, logout 공용)")
public record RefreshRequest(
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0cmFpbmVyMDEi...") @NotBlank(message = "리프레시 토큰을 입력해 주세요.") String refreshToken) {
}
