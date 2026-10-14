package com.trainergameserver.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "트레이너 선택 요청")
public record TrainerSelectRequest(
        @Schema(description = "보유한 트레이너 id", example = "1")
        @NotNull(message = "{validation.selectTrainerId.required}")
        Integer trainerId) {
}
