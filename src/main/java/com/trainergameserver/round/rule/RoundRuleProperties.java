package com.trainergameserver.round.rule;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 라운드 결과 검증 기준값 (서버 전용). application.properties 의 round.rule.* 에서 읽습니다.
 * 클라이언트에 내려주지 않습니다: 기준을 알면 그 바로 아래까지 조작할 수 있기 때문입니다.
 * 환경별로 바꾸려면 환경변수(예: ROUND_RULE_MAX_KILL_PER_SEC)로 덮어쓸 수 있습니다.
 * 값이 빠지거나 0 이하면 서버가 시작되지 않습니다(@Positive).
 */
@Validated
@ConfigurationProperties(prefix = "round.rule")
public record RoundRuleProperties(
        @Positive double stageDurationSec,    // 클리어에 필요한 생존 시간(초)
        @Positive double timeToleranceSec,    // 시간 비교 오차(초)
        @Positive double maxKillPerSec,       // 초당 최대 처치 수
        @Positive double maxExpPerKill,       // 야생 포켓몬 1마리 최대 경험치
        @Positive double maxBallPerKill,      // 야생 포켓몬 1마리 최대 몬스터볼
        @Positive double maxExpMultiplier,    // 업그레이드로 가능한 최대 경험치 배율
        @Positive double maxBallMultiplier,   // 업그레이드로 가능한 최대 몬스터볼 배율
        @Positive double rivalSpawnSec,       // 라이벌 출현 시간 (클라와 같은 값이어야 함)
        @Positive double rivalMinKillSec,     // 라이벌 체력 / 이론상 최대 DPS
        @Positive double rivalExp,
        @Positive double rivalBall,
        @Positive double championSpawnSec,    // 챔피언 출현 시간 (클라와 같은 값이어야 함)
        @Positive double championMinKillSec,
        @Positive double championExp,
        @Positive double championBall) {
}
