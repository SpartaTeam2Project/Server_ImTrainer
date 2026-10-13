package com.trainergameserver.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "인트로 완료 요청")
public record IntroRequest(
        @Schema(description = "닉네임 (1~20자)", example = "레드")
        @NotBlank(message = "{validation.nickname.required}")
        @Size(max = 20, message = "{validation.nickname.size}")
        String nickname,

        @Schema(description = "성별. 0 남자, 1 여자", example = "0")
        @NotNull(message = "{validation.gender.required}")
        @Min(value = 0, message = "{validation.gender.range}")
        @Max(value = 1, message = "{validation.gender.range}")
        Integer gender,

        @Schema(description = "세대", example = "1")
        @NotNull(message = "{validation.generation.required}")
        Integer generation,

        @Schema(description = "스타터 포켓몬 id (storage_monster에 있는 id)", example = "1")
        @NotNull(message = "{validation.starterMonsterId.required}")
        Integer starterMonsterId,

        @Schema(description = "고른 트레이너 id (인트로 지급 대상 중 하나)", example = "1")
        @NotNull(message = "{validation.trainerId.required}")
        Integer trainerId) {
}
