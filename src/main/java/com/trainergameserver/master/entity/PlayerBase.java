package com.trainergameserver.master.entity;

import com.trainergameserver.master.dto.PlayerBaseData;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 플레이어 기본 스탯 마스터데이터. 행은 id=1 하나뿐입니다. 값은 resources/masterdata/player_base.json에서 반영됩니다. */
@Entity
@Table(name = "player_base")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerBase {

    public static final int SINGLE_ID = 1;

    @Id
    private Integer id;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal moveSpeed;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal damageMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal magnetRadius;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal xpMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal hpRegen;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal cooldownMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal projectileSpeedMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal projectileMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal sizeMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal durationMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal invincibleSeconds;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal maxHealth;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal reviveHealthRatio;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal reviveInvincibleSeconds;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal baseRequiredXp;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal requiredGrowth;

    public PlayerBase(Integer id) {
        this.id = id;
    }

    public void apply(PlayerBaseData data) {
        this.moveSpeed = data.moveSpeed();
        this.damageMultiplier = data.damageMultiplier();
        this.magnetRadius = data.magnetRadius();
        this.xpMultiplier = data.xpMultiplier();
        this.hpRegen = data.hpRegen();
        this.cooldownMultiplier = data.cooldownMultiplier();
        this.projectileSpeedMultiplier = data.projectileSpeedMultiplier();
        this.projectileMultiplier = data.projectileMultiplier();
        this.sizeMultiplier = data.sizeMultiplier();
        this.durationMultiplier = data.durationMultiplier();
        this.invincibleSeconds = data.invincibleSeconds();
        this.maxHealth = data.maxHealth();
        this.reviveHealthRatio = data.reviveHealthRatio();
        this.reviveInvincibleSeconds = data.reviveInvincibleSeconds();
        this.baseRequiredXp = data.baseRequiredXp();
        this.requiredGrowth = data.requiredGrowth();
    }
}
