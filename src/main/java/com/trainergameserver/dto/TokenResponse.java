package com.trainergameserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 발급 응답. 두 토큰 모두 보안 저장소에 저장하고, refresh 성공 시 모두 새 값으로 교체합니다.")
public record TokenResponse(
        @Schema(description = "액세스 토큰. API 호출 시 Authorization: Bearer {accessToken}", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0cmFpbmVyMDEi...")
        String accessToken,

        @Schema(description = "리프레시 토큰. 액세스 토큰 재발급에 사용", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0cmFpbmVyMDEi...")
        String refreshToken,

        @Schema(description = "액세스 토큰 만료까지 남은 시간(초)", example = "1800")
        long expiresIn) {
}
