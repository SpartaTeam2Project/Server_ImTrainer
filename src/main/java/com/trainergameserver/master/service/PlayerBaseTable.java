package com.trainergameserver.master.service;

import com.trainergameserver.master.dto.PlayerBaseData;
import com.trainergameserver.master.entity.PlayerBase;
import com.trainergameserver.master.repository.PlayerBaseRepository;
import org.springframework.stereotype.Component;

/** 플레이어 기본 스탯 (player_base.json → player_base 테이블, 행 1개) */
@Component
public class PlayerBaseTable extends MasterTable<PlayerBaseData> {

    private final PlayerBaseRepository playerBaseRepository;

    public PlayerBaseTable(PlayerBaseRepository playerBaseRepository) {
        this.playerBaseRepository = playerBaseRepository;
    }

    @Override
    public String name() {
        return "player_base";
    }

    @Override
    public Class<PlayerBaseData> type() {
        return PlayerBaseData.class;
    }

    @Override
    protected void validate(PlayerBaseData file) {
        requireScale(file, file.values());
    }

    @Override
    protected void sync(PlayerBaseData file) {
        PlayerBase base = playerBaseRepository.findById(PlayerBase.SINGLE_ID)
                .orElseGet(() -> new PlayerBase(PlayerBase.SINGLE_ID));
        base.apply(file);
        playerBaseRepository.save(base);
    }

    @Override
    protected PlayerBaseData load() {
        PlayerBase b = playerBaseRepository.findById(PlayerBase.SINGLE_ID)
                .orElseThrow(() -> new IllegalStateException("player_base 테이블이 비어 있습니다."));
        return new PlayerBaseData(
                plain(b.getMoveSpeed()), plain(b.getDamageMultiplier()), plain(b.getMagnetRadius()),
                plain(b.getXpMultiplier()), plain(b.getHpRegen()), plain(b.getCooldownMultiplier()),
                plain(b.getProjectileSpeedMultiplier()), plain(b.getProjectileMultiplier()),
                plain(b.getSizeMultiplier()), plain(b.getDurationMultiplier()), plain(b.getInvincibleSeconds()),
                plain(b.getMaxHealth()), plain(b.getReviveHealthRatio()), plain(b.getReviveInvincibleSeconds()),
                plain(b.getBaseRequiredXp()), plain(b.getRequiredGrowth()));
    }
}
