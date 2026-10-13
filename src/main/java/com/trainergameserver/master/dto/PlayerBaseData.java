package com.trainergameserver.master.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/** player_base.json: 플레이어 기본 스탯 (단일 객체). 필드명·순서는 클라가 준 JSON 그대로입니다. */
@JsonPropertyOrder({"moveSpeed", "damageMultiplier", "magnetRadius", "xpMultiplier", "hpRegen",
        "cooldownMultiplier", "projectileSpeedMultiplier", "projectileMultiplier", "sizeMultiplier",
        "durationMultiplier", "invincibleSeconds", "maxHealth", "reviveHealthRatio",
        "reviveInvincibleSeconds", "baseRequiredXp", "requiredGrowth"})
public record PlayerBaseData(
        BigDecimal moveSpeed,
        BigDecimal damageMultiplier,
        BigDecimal magnetRadius,
        BigDecimal xpMultiplier,
        BigDecimal hpRegen,
        BigDecimal cooldownMultiplier,
        BigDecimal projectileSpeedMultiplier,
        BigDecimal projectileMultiplier,
        BigDecimal sizeMultiplier,
        BigDecimal durationMultiplier,
        BigDecimal invincibleSeconds,
        BigDecimal maxHealth,
        BigDecimal reviveHealthRatio,
        BigDecimal reviveInvincibleSeconds,
        BigDecimal baseRequiredXp,
        BigDecimal requiredGrowth) {

    /** 검증, 저장에 쓰는 전체 값 (선언 순서) */
    @JsonIgnore
    public BigDecimal[] values() {
        return new BigDecimal[]{moveSpeed, damageMultiplier, magnetRadius, xpMultiplier, hpRegen,
                cooldownMultiplier, projectileSpeedMultiplier, projectileMultiplier, sizeMultiplier,
                durationMultiplier, invincibleSeconds, maxHealth, reviveHealthRatio,
                reviveInvincibleSeconds, baseRequiredXp, requiredGrowth};
    }
}
