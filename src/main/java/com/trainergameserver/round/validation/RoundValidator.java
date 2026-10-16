package com.trainergameserver.round.validation;

import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.master.dto.PlayerBaseData;
import com.trainergameserver.master.dto.RoundRuleData;
import com.trainergameserver.round.dto.BossTime;
import com.trainergameserver.round.dto.MonsterBall;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/**
 * 라운드 결과 검증. 기준값은 마스터데이터(round_rule, player_base)에서 읽습니다.
 * 실패하면 400 + 항목별 ErrorCode. 응답 문구는 모두 같고, 자세한 이유(reason)는 서버 로그에만 남습니다.
 */
@Component
public class RoundValidator {

    /** 레벨 계산 상한 (곡선 값이 잘못돼도 무한 루프가 되지 않도록) */
    private static final int MAX_LEVEL = 500;

    public void validate(RoundEndRequest req, RoundValidationContext ctx) {
        RoundRuleData rule = ctx.rule();
        validateTime(req, rule, ctx.serverElapsedSec());
        validateBosses(req, rule);
        validateKill(req, rule);
        validateExpAndLevel(req, rule, ctx.playerBase());
        validateMonsterBall(req, rule);
        validateEntry(req, ctx);
    }

    /**
     * 누적 경험치로 레벨 계산. 레벨 L → L+1 에 필요한 경험치 = baseRequiredXp × requiredGrowth^(L-1).
     * 클라(Unity)의 레벨업 공식과 반드시 같아야 합니다.
     */
    public static int levelOf(long totalExp, PlayerBaseData base) {
        double need = d(base.baseRequiredXp());
        double growth = d(base.requiredGrowth());
        double remaining = totalExp;
        int level = 1;
        while (level < MAX_LEVEL && remaining >= need) {
            remaining -= need;
            need *= growth;
            level++;
        }
        return level;
    }

    // INVALID_TIME
    private void validateTime(RoundEndRequest req, RoundRuleData rule, double serverElapsedSec) {
        double tolerance = d(rule.timeToleranceSec());
        // 일시정지가 있으므로 게임 시간은 실제 경과 시간보다 길 수 없다
        if (req.gameTime() > serverElapsedSec + tolerance) {
            fail(ErrorCode.INVALID_TIME, "gameTime %.1f > 서버 경과 %.1f".formatted(req.gameTime(), serverElapsedSec));
        }
        // 클리어 조건: 제한 시간 생존 + 챔피언 처치
        if (req.result() == RoundResult.CLEAR) {
            double duration = d(rule.stageDurationSec());
            if (req.gameTime() < duration)
                fail(ErrorCode.INVALID_TIME, "CLEAR 인데 gameTime %.1f < %.0f".formatted(req.gameTime(), duration));
            if (req.bossTime().championKill() == null)
                fail(ErrorCode.INVALID_TIME, "CLEAR 인데 챔피언 미처치");
        }
    }

    // INVALID_BOSS
    private void validateBosses(RoundEndRequest req, RoundRuleData rule) {
        BossTime b = req.bossTime();
        double tolerance = d(rule.timeToleranceSec());
        checkBoss("라이벌", b.rivalSpawn(), b.rivalKill(), d(rule.rivalSpawnSec()), d(rule.rivalMinKillSec()), tolerance, req.gameTime());
        checkBoss("챔피언", b.championSpawn(), b.championKill(), d(rule.championSpawnSec()), d(rule.championMinKillSec()), tolerance, req.gameTime());
    }

    private void checkBoss(String name, Double spawn, Double kill, double spawnSec, double minKillSec,
                           double tolerance, double gameTime) {
        if (spawn == null && kill != null)
            fail(ErrorCode.INVALID_BOSS, name + " 출현 없이 처치 기록");
        if (spawn == null && gameTime >= spawnSec + tolerance)
            fail(ErrorCode.INVALID_BOSS, "%s 출현 시간(%.0f) 넘겨 생존했는데 출현 기록 없음".formatted(name, spawnSec));

        if (spawn != null) {
            if (Math.abs(spawn - spawnSec) > tolerance)
                fail(ErrorCode.INVALID_BOSS, "%s 출현 %.1f, 예정 %.0f".formatted(name, spawn, spawnSec));
            if (spawn > gameTime)
                fail(ErrorCode.INVALID_BOSS, "%s 출현 %.1f > gameTime %.1f".formatted(name, spawn, gameTime));
        }
        if (kill != null) {
            if (kill > gameTime)
                fail(ErrorCode.INVALID_BOSS, "%s 처치 %.1f > gameTime %.1f".formatted(name, kill, gameTime));
            if (kill - spawn < minKillSec)
                fail(ErrorCode.INVALID_BOSS, "%s 처치 소요 %.1f초 < 최소 %.0f초".formatted(name, kill - spawn, minKillSec));
        }
    }

    // INVALID_KILL
    private void validateKill(RoundEndRequest req, RoundRuleData rule) {
        double maxKills = req.gameTime() * d(rule.maxKillPerSec());
        if (req.killCount() > maxKills)
            fail(ErrorCode.INVALID_KILL, "killCount %d > 최대 %.0f".formatted(req.killCount(), maxKills));
    }

    // INVALID_EXP, INVALID_LEVEL
    private void validateExpAndLevel(RoundEndRequest req, RoundRuleData rule, PlayerBaseData base) {
        double maxExp = req.killCount() * d(rule.maxExpPerKill()) * d(rule.maxExpMultiplier())
                + bossReward(req.bossTime(), d(rule.rivalExp()), d(rule.championExp())) * d(rule.maxExpMultiplier());
        if (req.totalExp() > maxExp)
            fail(ErrorCode.INVALID_EXP, "totalExp %d > 최대 %.0f".formatted(req.totalExp(), maxExp));

        int expected = levelOf(req.totalExp(), base);
        if (expected != req.level())
            fail(ErrorCode.INVALID_LEVEL, "level %d, totalExp %d 기준 %d".formatted(req.level(), req.totalExp(), expected));
    }

    // INVALID_MONSTERBALL
    private void validateMonsterBall(RoundEndRequest req, RoundRuleData rule) {
        MonsterBall m = req.monsterBall();
        if (m.earned() - m.spent() + m.sold() != m.current())
            fail(ErrorCode.INVALID_MONSTERBALL, "산식 불일치 %d - %d + %d != %d".formatted(m.earned(), m.spent(), m.sold(), m.current()));

        double maxEarned = req.killCount() * d(rule.maxBallPerKill()) * d(rule.maxBallMultiplier())
                + bossReward(req.bossTime(), d(rule.rivalBall()), d(rule.championBall())) * d(rule.maxBallMultiplier());
        if (m.earned() > maxEarned)
            fail(ErrorCode.INVALID_MONSTERBALL, "earned %d > 최대 %.0f".formatted(m.earned(), maxEarned));
    }

    // INVALID_ENTRY (개수 제한은 @Size → INVALID_REQUEST)
    private void validateEntry(RoundEndRequest req, RoundValidationContext ctx) {
        List<Integer> entry = req.pokemonEntry();
        if (new HashSet<>(entry).size() != entry.size())
            fail(ErrorCode.INVALID_ENTRY, "중복 " + entry);
        for (Integer id : entry) {
            if (!ctx.ownedMonsterIds().contains(id))
                fail(ErrorCode.INVALID_ENTRY, "보유하지 않은 포켓몬 " + id + " (보유: " + ctx.ownedMonsterIds() + ")");
        }
    }

    /** 처치한 보스의 보상 합계 (배율 적용 전) */
    private static double bossReward(BossTime b, double rival, double champion) {
        return (b.rivalKill() != null ? rival : 0) + (b.championKill() != null ? champion : 0);
    }

    private static double d(BigDecimal value) {
        return value.doubleValue();
    }

    /** reason 은 서버 로그에만 남고 응답에는 나가지 않습니다 */
    private void fail(ErrorCode code, String reason) {
        throw new RoundValidationException(code, reason);
    }
}
