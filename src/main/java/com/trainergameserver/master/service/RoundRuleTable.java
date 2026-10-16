package com.trainergameserver.master.service;

import com.trainergameserver.master.dto.RoundRuleData;
import com.trainergameserver.master.entity.RoundRule;
import com.trainergameserver.master.repository.RoundRuleRepository;
import org.springframework.stereotype.Component;

/** 라운드 결과 검증 기준값 (round_rule.json → round_rule 테이블, 행 1개) */
@Component
public class RoundRuleTable extends MasterTable<RoundRuleData> {

    private final RoundRuleRepository roundRuleRepository;

    public RoundRuleTable(RoundRuleRepository roundRuleRepository) {
        this.roundRuleRepository = roundRuleRepository;
    }

    @Override
    public String name() {
        return "round_rule";
    }

    @Override
    public Class<RoundRuleData> type() {
        return RoundRuleData.class;
    }

    @Override
    protected void validate(RoundRuleData file) {
        requireScale(file, file.values());
        for (var value : file.values()) {
            if (value.signum() < 0) {
                throw new IllegalStateException(filePath() + ": 음수 값이 있습니다. " + file);
            }
        }
    }

    @Override
    protected void sync(RoundRuleData file) {
        RoundRule rule = roundRuleRepository.findById(RoundRule.SINGLE_ID)
                .orElseGet(() -> new RoundRule(RoundRule.SINGLE_ID));
        rule.apply(file);
        roundRuleRepository.save(rule);
    }

    @Override
    protected RoundRuleData load() {
        RoundRule r = roundRuleRepository.findById(RoundRule.SINGLE_ID)
                .orElseThrow(() -> new IllegalStateException("round_rule 테이블이 비어 있습니다."));
        return new RoundRuleData(
                plain(r.getStageDurationSec()), plain(r.getTimeToleranceSec()), plain(r.getMaxKillPerSec()),
                plain(r.getMaxExpPerKill()), plain(r.getMaxBallPerKill()), plain(r.getMaxExpMultiplier()),
                plain(r.getMaxBallMultiplier()), plain(r.getRivalSpawnSec()), plain(r.getRivalMinKillSec()),
                plain(r.getRivalExp()), plain(r.getRivalBall()), plain(r.getChampionSpawnSec()),
                plain(r.getChampionMinKillSec()), plain(r.getChampionExp()), plain(r.getChampionBall()));
    }
}
