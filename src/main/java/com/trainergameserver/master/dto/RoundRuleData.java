package com.trainergameserver.master.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

/**
 * round_rule.json: 라운드 결과 검증 기준값 (단일 객체).
 * 서버는 /rounds/end 검증에, 클라는 보스 출현 시간 등 같은 값을 쓰기 위해 받습니다.
 */
@JsonPropertyOrder({"stageDurationSec", "timeToleranceSec", "maxKillPerSec", "maxExpPerKill", "maxBallPerKill",
        "maxExpMultiplier", "maxBallMultiplier", "rivalSpawnSec", "rivalMinKillSec", "rivalExp", "rivalBall",
        "championSpawnSec", "championMinKillSec", "championExp", "championBall"})
public record RoundRuleData(
        BigDecimal stageDurationSec,    // 클리어에 필요한 생존 시간(초)
        BigDecimal timeToleranceSec,    // 시간 비교 오차(초)
        BigDecimal maxKillPerSec,       // 초당 최대 처치 수
        BigDecimal maxExpPerKill,       // 야생 포켓몬 1마리 최대 경험치
        BigDecimal maxBallPerKill,      // 야생 포켓몬 1마리 최대 몬스터볼
        BigDecimal maxExpMultiplier,    // 업그레이드로 가능한 최대 경험치 배율
        BigDecimal maxBallMultiplier,   // 업그레이드로 가능한 최대 몬스터볼 배율
        BigDecimal rivalSpawnSec,
        BigDecimal rivalMinKillSec,     // 라이벌 체력 / 이론상 최대 DPS
        BigDecimal rivalExp,
        BigDecimal rivalBall,
        BigDecimal championSpawnSec,
        BigDecimal championMinKillSec,
        BigDecimal championExp,
        BigDecimal championBall) {

    /** 검증, 저장에 쓰는 전체 값 (선언 순서) */
    @JsonIgnore
    public BigDecimal[] values() {
        return new BigDecimal[]{stageDurationSec, timeToleranceSec, maxKillPerSec, maxExpPerKill, maxBallPerKill,
                maxExpMultiplier, maxBallMultiplier, rivalSpawnSec, rivalMinKillSec, rivalExp, rivalBall,
                championSpawnSec, championMinKillSec, championExp, championBall};
    }
}
