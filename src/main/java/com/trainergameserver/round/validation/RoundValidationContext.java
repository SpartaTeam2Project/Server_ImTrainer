package com.trainergameserver.round.validation;

import com.trainergameserver.master.dto.PlayerBaseData;
import com.trainergameserver.round.rule.RoundRuleProperties;

import java.util.Set;

/**
 * 검증에 필요한 서버 쪽 값. RoundService가 모아서 넘기고, RoundValidator는 이 값만으로 판단합니다(DB·Redis 없이 테스트 가능).
 *
 * @param serverElapsedSec 서버가 잰 경과 시간 (종료 요청 시각 - 시작 요청 시각)
 * @param rule             라운드 검증 기준값 (application.properties round.rule.*)
 * @param playerBase       player_base 마스터데이터 (레벨 곡선: baseRequiredXp, requiredGrowth)
 * @param ownedMonsterIds  플레이어가 보유한 포켓몬 id
 * @param knownTrainerIds  마스터데이터 trainer 에 있는 트레이너 id
 * @param knownMonsterIds  마스터데이터 storage_monster 에 있는 포켓몬 id
 */
public record RoundValidationContext(
        double serverElapsedSec,
        RoundRuleProperties rule,
        PlayerBaseData playerBase,
        Set<Integer> ownedMonsterIds,
        Set<Integer> knownTrainerIds,
        Set<Integer> knownMonsterIds) {
}
