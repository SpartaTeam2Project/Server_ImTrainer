package com.trainergameserver.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "트레이닝 초기화 요청 (클라 계산 결과)")
public record TrainingResetRequest(
        @Schema(description = "환불액 (산 레벨 비용의 합)", example = "910")
        @NotNull(message = "{validation.refund.required}")
        Long refund,

        @Schema(description = "초기화 후 잔액 (잔액 + refund)", example = "2110")
        @NotNull(message = "{validation.resetBalanceAfter.required}")
        Long balanceAfter) {
}
