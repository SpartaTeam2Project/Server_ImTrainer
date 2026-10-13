package com.trainergameserver.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "아이디 (게임 내 이름으로도 사용, 중복 불가)", example = "trainer01")
        @NotBlank(message = "{validation.username.required}")
        @Size(min = 4, max = 20, message = "{validation.username.size}")
        String username,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 20, message = "{validation.password.size}")
        String password) {
}
