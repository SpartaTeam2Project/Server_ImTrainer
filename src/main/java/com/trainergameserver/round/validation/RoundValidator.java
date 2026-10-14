package com.trainergameserver.round.validation;

import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.round.dto.BossTime;
import com.trainergameserver.round.dto.MonsterBall;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundResult;
import com.trainergameserver.round.validation.GameData.BossData;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * 라운드 결과 검증. 실패하면 400 + 항목별 ErrorCode.
 * 메시지는 조작 힌트가 되지 않도록 messages.properties 에서 모두 같은 문구를 쓴다.
 */
@Component
public class RoundValidator {

    private final GameData gameData;

    public RoundValidator(GameData gameData) {
        this.gameData = gameData;
    }

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
        // 일시정지가 있으므로 게임 시간은 실제 경과 시간보다 길 수 없다
        if (req.gameTime() > serverElapsedSec + GameData.TIME_TOLERANCE_SEC) {
            fail(ErrorCode.INVALID_TIME);
        }
        // 클리어 조건: 제한 시간 생존 + 챔피언 처치
        if (req.result() == RoundResult.CLEAR) {
            if (req.gameTime() < GameData.STAGE_DURATION_SEC) fail(ErrorCode.INVALID_TIME);
            if (req.bossTime().championKill() == null) fail(ErrorCode.INVALID_TIME);
        }
    }

    // INVALID_BOSS
    private void validateBosses(RoundEndRequest req) {
        BossTime b = req.bossTime();
        checkBoss(b.rivalSpawn(), b.rivalKill(), GameData.RIVAL, req.gameTime());
        checkBoss(b.championSpawn(), b.championKill(), GameData.CHAMPION, req.gameTime());
    }

    private void checkBoss(Double spawn, Double kill, BossData data, double gameTime) {
        // 출현하지 않았는데 처치 기록이 있음
        if (spawn == null && kill != null) fail(ErrorCode.INVALID_BOSS);
        // 출현 예정 시간을 넘겨 생존했는데 출현 기록이 없음
        if (spawn == null && gameTime >= data.spawnTime() + GameData.TIME_TOLERANCE_SEC) fail(ErrorCode.INVALID_BOSS);

        if (spawn != null) {
            if (Math.abs(spawn - data.spawnTime()) > GameData.TIME_TOLERANCE_SEC) fail(ErrorCode.INVALID_BOSS);
            if (spawn > gameTime) fail(ErrorCode.INVALID_BOSS);
        }
        if (kill != null) {
            if (kill > gameTime) fail(ErrorCode.INVALID_BOSS);
            // 이론상 최소 처치 시간보다 빠름
            if (kill - spawn < data.minKillSec()) fail(ErrorCode.INVALID_BOSS);
        }
    }

    // INVALID_KILL
    private void validateKill(RoundEndRequest req) {
        double maxKills = req.gameTime() * GameData.MAX_KILL_PER_SEC;
        if (req.killCount() > maxKills) fail(ErrorCode.INVALID_KILL);
    }

    // INVALID_EXP, INVALID_LEVEL
    private void validateExpAndLevel(RoundEndRequest req) {
        double maxExp = (double) req.killCount() * GameData.MAX_EXP_PER_KILL * GameData.MAX_EXP_MULTIPLIER
                + bossReward(req.bossTime(), true);
        if (req.totalExp() > maxExp) fail(ErrorCode.INVALID_EXP);

        if (gameData.levelOf(req.totalExp()) != req.level()) fail(ErrorCode.INVALID_LEVEL);
    }

    // INVALID_MONSTERBALL
    private void validateMonsterBall(RoundEndRequest req) {
        MonsterBall m = req.monsterBall();
        if (m.earned() - m.spent() + m.sold() != m.current()) fail(ErrorCode.INVALID_MONSTERBALL);

        double maxEarned = (double) req.killCount() * GameData.MAX_BALL_PER_KILL * GameData.MAX_BALL_MULTIPLIER
                + bossReward(req.bossTime(), false);
        if (m.earned() > maxEarned) fail(ErrorCode.INVALID_MONSTERBALL);
    }

    // INVALID_ENTRY (개수 제한은 @Size → INVALID_REQUEST)
    private void validateEntry(RoundEndRequest req) {
        List<Integer> entry = req.pokemonEntry();
        if (new HashSet<>(entry).size() != entry.size()) fail(ErrorCode.INVALID_ENTRY);
        for (Integer id : entry) {
            if (!gameData.pokemonExists(id)) fail(ErrorCode.INVALID_ENTRY);
        }
    }

    /** 처치한 보스의 보상 합계 (exp = true 면 경험치, false 면 몬스터볼). 배율 적용 */
    private double bossReward(BossTime b, boolean exp) {
        double sum = 0;
        if (b.rivalKill() != null) sum += exp ? GameData.RIVAL.exp() : GameData.RIVAL.ball();
        if (b.championKill() != null) sum += exp ? GameData.CHAMPION.exp() : GameData.CHAMPION.ball();
        return sum * (exp ? GameData.MAX_EXP_MULTIPLIER : GameData.MAX_BALL_MULTIPLIER);
    }

    private void fail(ErrorCode code) {
        throw new ApiException(code);
    }
}
