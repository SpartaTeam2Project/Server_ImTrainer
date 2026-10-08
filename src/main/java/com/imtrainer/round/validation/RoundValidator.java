package com.imtrainer.round.validation;

import com.imtrainer.round.data.GameData;
import com.imtrainer.round.dto.BossTime;
import com.imtrainer.round.dto.MonsterBall;
import com.imtrainer.round.dto.RoundEndRequest;
import com.imtrainer.round.dto.RoundResult;
import com.imtrainer.round.exception.RoundValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.imtrainer.round.data.GameData.BossData;

import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoundValidator {
    private final GameData gameData;

    public void validate(RoundEndRequest req, double serverElapsedSec) {
        validateTime(req, serverElapsedSec);
        validateBosses(req);
        validateKill(req);
        validateExpAndLevel(req);
        validateMonsterBall(req);
        validateEntry(req);
    }

    // INVALID_TIME
    private void validateTime(RoundEndRequest req, double serverElapsedSec) {
        if (req.gameTime() > serverElapsedSec + GameData.TIME_TOLERANCE_SEC) {
            fail("INVALID_TIME");
        }
        // 클리어 조건 : 제한 시간 생존 및 챔피언 처치
        if (req.result() == RoundResult.CLEAR && req.gameTime() < GameData.STAGE_DURATION_SEC) {
            fail("INVALID_TIME");
        }
    }

    // INVALID_BOSS
    private void validateBosses(RoundEndRequest req) {
        BossTime b = req.bossTime();
        checkBoss(b.rivalSpawn(), b.rivalKill(), GameData.RIVAL, req.gameTime());
        checkBoss(b.championSpawn(), b.championKill(), GameData.CHAMPION, req.gameTime());
    }

    private void checkBoss(Double spawn, Double kill, BossData data, double gameTime) {
        if (spawn == null && kill != null) fail("INVALID_BOSS");

        if (spawn == null && gameTime >= data.spawnTime() + GameData.TIME_TOLERANCE_SEC) {
            fail("INVALID_BOSS");
        }

        if (spawn != null) {
            // 출현 시간이 스케줄과 다름
            if (Math.abs(spawn - data.spawnTime()) > GameData.TIME_TOLERANCE_SEC) fail("INVALID_BOSS");
            if (spawn > gameTime) fail("INVALID_BOSS");
        }

        if (kill != null) {
            if (kill > gameTime) fail("INVALID_BOSS");
            // 이론상 최소 처치 시간보다 빠름
            if (kill - spawn < data.minKillSec()) fail("INVALID_BOSS");
        }
    }

    // INVALID_KILL
    private void validateKill(RoundEndRequest req) {
        double maxKills = req.gameTime() * GameData.MAX_KILL_PER_SEC;
        if (req.killCount() > maxKills) fail("INVALID_KILL");
    }

    // INVALID_EXP, INVALID_LEVEL
    private void validateExpAndLevel(RoundEndRequest req) {
        double maxExp = (double) req.killCount() * GameData.MAX_EXP_PER_KILL * GameData.MAX_EXP_MULTIPLIER
                + bossReward(req.bossTime(), true);
        if (req.totalExp() > maxExp) fail("INVALID_EXP");

        if (gameData.levelOf(req.totalExp()) != req.level()) fail("INVALID_LEVEL");
    }

    // INVALID_MONSTERBALL
    private void validateMonsterBall(RoundEndRequest req) {
        MonsterBall m = req.monsterBall();
        if (m.earned() - m.spent() + m.sold() != m.current()) fail("INVALID_MONSTERBALL");

        double maxEarned = (double) req.killCount() * GameData.MAX_BALL_PER_KILL * GameData.MAX_BALL_MULTIPLIER
                + bossReward(req.bossTime(), false);
        if (m.earned() > maxEarned) fail("INVALID_MONSTERBALL");
    }

    // INVALID_ENTRY
    private void validateEntry(RoundEndRequest req) {
        List<Integer> entry = req.pokemonEntry();
        if (new HashSet<>(entry).size() != entry.size()) fail("INVALID_ENTRY");
        for (Integer id : entry) {
            if (!gameData.pokemonExists(id)) fail("INVALID_ENTRY");
        }
    }

    // 처치한 보스의 보상 합계 (exp = true 면 경험치, false 면 몬스터볼). 배율 적용
    private double bossReward(BossTime b, boolean exp) {
        double sum = 0;
        if (b.rivalKill() != null) sum += exp ? GameData.RIVAL.exp() : GameData.RIVAL.ball();
        if (b.championKill() != null) sum += exp ? GameData.CHAMPION.exp() : GameData.CHAMPION.ball();
        return sum * (exp ? GameData.MAX_EXP_MULTIPLIER : GameData.MAX_BALL_MULTIPLIER);
    }

    private void fail(String code) {
        throw new RoundValidationException(code);
    }
}
