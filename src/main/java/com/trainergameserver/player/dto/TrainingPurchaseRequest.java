package com.trainergameserver.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "트레이닝 구매 요청 (클라 계산 결과)")
public record TrainingPurchaseRequest(
        @Schema(description = "트레이닝 id", example = "6")
        @NotNull(message = "{validation.trainingId.required}")
        Integer trainingId,

        @Schema(description = "구매 후 레벨 (현재 레벨 + 1)", example = "1")
        @NotNull(message = "{validation.toLevel.required}")
        Integer toLevel,

        @Schema(description = "비용 (levels[toLevel-1].cost)", example = "260")
        @NotNull(message = "{validation.cost.required}")
        Long cost,

        @Schema(description = "재화 id", example = "pocket_dollar")
        @NotBlank(message = "{validation.currencyId.required}")
        String currencyId,

        @Schema(description = "구매 후 잔액 (잔액 - cost)", example = "940")
        @NotNull(message = "{validation.balanceAfter.required}")
        Long balanceAfter) {
}
