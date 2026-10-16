package com.trainergameserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "아이디", example = "trainer01")
        @NotBlank(message = "{validation.username.required}")
        String username,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "{validation.password.required}")
        String password) {
}
