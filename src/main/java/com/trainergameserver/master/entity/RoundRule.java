package com.trainergameserver.master.entity;

import com.trainergameserver.master.dto.RoundRuleData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 라운드 결과 검증 기준값 마스터데이터. 행은 id=1 하나뿐입니다. 값은 resources/masterdata/round_rule.json에서 반영됩니다. */
@Entity
@Table(name = "round_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoundRule {

    public static final int SINGLE_ID = 1;

    @Id
    private Integer id;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal stageDurationSec;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal timeToleranceSec;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal maxKillPerSec;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal maxExpPerKill;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal maxBallPerKill;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal maxExpMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal maxBallMultiplier;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal rivalSpawnSec;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal rivalMinKillSec;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal rivalExp;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal rivalBall;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal championSpawnSec;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal championMinKillSec;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal championExp;
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal championBall;

    public RoundRule(Integer id) {
        this.id = id;
    }

    public void apply(RoundRuleData data) {
        this.stageDurationSec = data.stageDurationSec();
        this.timeToleranceSec = data.timeToleranceSec();
        this.maxKillPerSec = data.maxKillPerSec();
        this.maxExpPerKill = data.maxExpPerKill();
        this.maxBallPerKill = data.maxBallPerKill();
        this.maxExpMultiplier = data.maxExpMultiplier();
        this.maxBallMultiplier = data.maxBallMultiplier();
        this.rivalSpawnSec = data.rivalSpawnSec();
        this.rivalMinKillSec = data.rivalMinKillSec();
        this.rivalExp = data.rivalExp();
        this.rivalBall = data.rivalBall();
        this.championSpawnSec = data.championSpawnSec();
        this.championMinKillSec = data.championMinKillSec();
        this.championExp = data.championExp();
        this.championBall = data.championBall();
    }
}
