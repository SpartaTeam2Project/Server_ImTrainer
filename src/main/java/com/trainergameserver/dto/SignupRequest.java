package com.trainergameserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "아이디 (게임 내 이름으로도 사용, 중복 불가)", example = "trainer01")
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자로 입력해 주세요.")
        String username,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해 주세요.")
        String password) {
}
