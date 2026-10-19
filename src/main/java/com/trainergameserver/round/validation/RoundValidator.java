package com.trainergameserver.round.validation;

import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.master.dto.PlayerBaseData;
import com.trainergameserver.round.dto.BossTime;
import com.trainergameserver.round.dto.MonsterBall;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundResult;
import com.trainergameserver.round.rule.RoundRuleProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

/**
 * 라운드 결과 검증. 기준값은 서버 설정(round.rule.*)과 마스터데이터 player_base(레벨 곡선)에서 읽습니다.
 * 실패하면 400 + 항목별 ErrorCode. 응답 문구는 모두 같고, 자세한 이유(reason)는 서버 로그에만 남습니다.
 */
@Component
public class RoundValidator {

    /** 레벨 계산 상한 (곡선 값이 잘못돼도 무한 루프가 되지 않도록) */
    private static final int MAX_LEVEL = 500;

    /** 해금 검증 + 결과 검증 (테스트 등에서 한 번에 확인할 때) */
    public void validate(RoundEndRequest req, RoundValidationContext ctx) {
        validateUnlocks(req, ctx);
        validateResult(req, ctx);
    }

    /**
     * 해금 검증: 마스터데이터에 있는 id인지, 중복이 없는지.
     * 오탐이 날 수 없는 확실한 검사라, 실패하면 해금도 하지 않습니다.
     */
    public void validateUnlocks(RoundEndRequest req, RoundValidationContext ctx) {
        checkUnlock("트레이너", req.unlockedTrainerIds(), ctx.knownTrainerIds());
        checkUnlock("포켓몬", req.unlockedMonsterIds(), ctx.knownMonsterIds());
    }

    /**
     * 결과 검증: 시간, 보스, 처치 수, 경험치·레벨, 몬스터볼, 엔트리.
     * 기준값(round.rule.*)에 따라 정상 플레이도 걸릴 수 있어, 실패해도 해금은 유지하고 리더보드·재화만 주지 않습니다.
     */
    public void validateResult(RoundEndRequest req, RoundValidationContext ctx) {
        RoundRuleProperties rule = ctx.rule();
        validateTime(req, rule, ctx.serverElapsedSec());
        validateBosses(req, rule);
        validateKill(req, rule);
        validateExpAndLevel(req, rule, ctx.playerBase());
        validateMonsterBall(req, rule);
        validatePocketDollar(req, rule);
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
    private void validateTime(RoundEndRequest req, RoundRuleProperties rule, double serverElapsedSec) {
        double tolerance = rule.timeToleranceSec();
        // 일시정지가 있으므로 게임 시간은 실제 경과 시간보다 길 수 없다
        if (req.gameTime() > serverElapsedSec + tolerance) {
            fail(ErrorCode.INVALID_TIME, "gameTime %.1f > 서버 경과 %.1f".formatted(req.gameTime(), serverElapsedSec));
        }
        // 클리어 조건: 챔피언전(3마리) 처치. 처치 즉시 클라가 end 를 보내므로 게임 시간 ≈ 챔피언 처치 시간
        if (req.result() == RoundResult.CLEAR) {
            Double championKill = req.bossTime().championKill();
            if (championKill == null)
                fail(ErrorCode.INVALID_TIME, "CLEAR 인데 챔피언 미처치");
            if (Math.abs(req.gameTime() - championKill) > tolerance)
                fail(ErrorCode.INVALID_TIME, "CLEAR 인데 gameTime %.1f 가 챔피언 처치 %.1f 와 다름".formatted(req.gameTime(), championKill));
        }
    }

    // INVALID_BOSS
    private void validateBosses(RoundEndRequest req, RoundRuleProperties rule) {
        BossTime b = req.bossTime();
        double tolerance = rule.timeToleranceSec();
        checkBoss("라이벌", b.rivalSpawn(), b.rivalKill(), rule.rivalSpawnSec(), tolerance, req.gameTime());
        checkBoss("챔피언", b.championSpawn(), b.championKill(), rule.championSpawnSec(), tolerance, req.gameTime());
    }

    /**
     * 보스전(라이벌 2마리, 챔피언 3마리) 동안 게임 시계가 멈추므로 처치 시간은 출현 시간과 같을 수 있다.
     * 처치 시간은 보스를 모두 잡은 시점. 보스전에 실제로 걸린 시간은 서버 경과 시간 검사(INVALID_TIME)에 포함된다.
     */
    private void checkBoss(String name, Double spawn, Double kill, double spawnSec,
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
            if (kill < spawn)
                fail(ErrorCode.INVALID_BOSS, "%s 처치 %.1f < 출현 %.1f".formatted(name, kill, spawn));
        }
    }

    // INVALID_KILL
    private void validateKill(RoundEndRequest req, RoundRuleProperties rule) {
        double maxKills = req.gameTime() * rule.maxKillPerSec();
        if (req.killCount() > maxKills)
            fail(ErrorCode.INVALID_KILL, "killCount %d > 최대 %.0f".formatted(req.killCount(), maxKills));
    }

    // INVALID_EXP, INVALID_LEVEL
    private void validateExpAndLevel(RoundEndRequest req, RoundRuleProperties rule, PlayerBaseData base) {
        if (!rule.expCheckEnabled()) {
            return;   // 임시: 클라 레벨 공식과 맞출 때까지 검증 안 함
        }
        double maxExp = req.killCount() * rule.maxExpPerKill() * rule.maxExpMultiplier()
                + bossReward(req.bossTime(), rule.rivalExp(), rule.championExp()) * rule.maxExpMultiplier();
        if (req.totalExp() > maxExp)
            fail(ErrorCode.INVALID_EXP, "totalExp %d > 최대 %.0f".formatted(req.totalExp(), maxExp));

        int expected = levelOf(req.totalExp(), base);
        if (expected != req.level())
            fail(ErrorCode.INVALID_LEVEL, "level %d, totalExp %d 기준 %d".formatted(req.level(), req.totalExp(), expected));
    }

    // INVALID_MONSTERBALL
    private void validateMonsterBall(RoundEndRequest req, RoundRuleProperties rule) {
        MonsterBall m = req.monsterBall();
        if (m.earned() - m.spent() + m.sold() != m.current())
            fail(ErrorCode.INVALID_MONSTERBALL, "산식 불일치 %d - %d + %d != %d".formatted(m.earned(), m.spent(), m.sold(), m.current()));

        double maxEarned = req.killCount() * rule.maxBallPerKill() * rule.maxBallMultiplier()
                + bossReward(req.bossTime(), rule.rivalBall(), rule.championBall()) * rule.maxBallMultiplier();
        if (m.earned() > maxEarned)
            fail(ErrorCode.INVALID_MONSTERBALL, "earned %d > 최대 %.0f".formatted(m.earned(), maxEarned));
    }

    // INVALID_POCKET_DOLLAR: 주운 포켓달러(ShardBonus 적용 전)가 처치 수·보스 기준 최대치를 넘는지
    private void validatePocketDollar(RoundEndRequest req, RoundRuleProperties rule) {
        double max = req.killCount() * rule.maxDollarPerKill() * rule.maxDollarMultiplier()
                + bossReward(req.bossTime(), rule.rivalDollar(), rule.championDollar()) * rule.maxDollarMultiplier();
        if (req.pocketDollar() > max)
            fail(ErrorCode.INVALID_POCKET_DOLLAR, "pocketDollar %d > 최대 %.0f".formatted(req.pocketDollar(), max));
    }

    // INVALID_UNLOCK (중복·마스터데이터에 없는 id)
    private void checkUnlock(String kind, List<Integer> ids, java.util.Set<Integer> known) {
        if (new HashSet<>(ids).size() != ids.size())
            fail(ErrorCode.INVALID_UNLOCK, kind + " 해금 목록 중복 " + ids);
        for (Integer id : ids) {
            if (!known.contains(id))
                fail(ErrorCode.INVALID_UNLOCK, "마스터데이터에 없는 " + kind + " " + id);
        }
    }

    // INVALID_ENTRY (개수 제한은 @Size → INVALID_REQUEST)
    // 엔트리는 이미 보유한 포켓몬 + 이번 라운드에서 해금한 포켓몬 중에서만 허용
    private void validateEntry(RoundEndRequest req, RoundValidationContext ctx) {
        List<Integer> entry = req.pokemonEntry();
        if (new HashSet<>(entry).size() != entry.size())
            fail(ErrorCode.INVALID_ENTRY, "중복 " + entry);
        for (Integer id : entry) {
            if (!ctx.ownedMonsterIds().contains(id) && !req.unlockedMonsterIds().contains(id))
                fail(ErrorCode.INVALID_ENTRY, "보유·해금하지 않은 포켓몬 " + id
                        + " (보유: " + ctx.ownedMonsterIds() + ", 해금: " + req.unlockedMonsterIds() + ")");
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
