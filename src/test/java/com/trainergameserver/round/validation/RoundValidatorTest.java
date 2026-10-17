package com.trainergameserver.round.validation;

import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.master.dto.PlayerBaseData;
import com.trainergameserver.round.dto.BossTime;
import com.trainergameserver.round.dto.MonsterBall;
import com.trainergameserver.round.dto.RoundEndRequest;
import com.trainergameserver.round.dto.RoundResult;
import com.trainergameserver.round.rule.RoundRuleProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RoundValidator 단위 테스트. Spring/Redis/MySQL 없이 검증 로직만 확인합니다.
 * 기준값은 application.properties 의 round.rule.*, masterdata/player_base.json 과 같은 값으로 둡니다.
 */
class RoundValidatorTest {

    private static final RoundRuleProperties RULE = new RoundRuleProperties(
            3, 20, 30, 3, 10, 2.0, 2.0, 2.0,
            600, 500, 50, 100,
            1200, 1500, 150, 300);

    /** 레벨 곡선만 의미 있음: baseRequiredXp 5, requiredGrowth 1.25 */
    private static final PlayerBaseData BASE = new PlayerBaseData(
            bd(2.5), bd(1), bd(1), bd(1), bd(0), bd(1), bd(1), bd(1), bd(1), bd(1), bd(0),
            bd(30), bd(0.5), bd(1.5), bd(5), bd(1.25));

    private static final Set<Integer> OWNED = Set.of(25, 6, 9, 1);
    private static final Set<Integer> KNOWN_TRAINERS = Set.of(1, 2, 3, 4, 5);
    private static final Set<Integer> KNOWN_MONSTERS = Set.of(1, 4, 6, 7, 9, 10, 25, 133, 150);
    private static final double SERVER_ELAPSED = 1900;

    private final RoundValidator validator = new RoundValidator();

    // ===== 정상 =====

    @Test
    @DisplayName("정상 클리어 기록은 통과한다")
    void validClear() {
        assertPass(request(r -> {}), SERVER_ELAPSED);
    }

    @Test
    @DisplayName("보스 출현 전에 사망한 기록은 통과한다")
    void validEarlyDeath() {
        assertPass(request(r -> {
            r.result = RoundResult.DEAD;
            r.gameTime = 300;
            r.bossTime = new BossTime(null, null, null, null);
        }), 320);
    }

    @Test
    @DisplayName("라이벌 처치 후 챔피언 출현 전에 사망한 기록은 통과한다")
    void validDeathAfterRival() {
        assertPass(request(r -> {
            r.result = RoundResult.DEAD;
            r.gameTime = 900;
            r.bossTime = new BossTime(600.0, 640.2, null, null);
        }), 950);
    }

    // ===== 레벨 곡선 =====

    @Test
    @DisplayName("레벨 곡선: 1→2 에 5, 2→3 에 6.25 (5 × 1.25)")
    void levelCurve() {
        assertThat(RoundValidator.levelOf(0, BASE)).isEqualTo(1);
        assertThat(RoundValidator.levelOf(4, BASE)).isEqualTo(1);
        assertThat(RoundValidator.levelOf(5, BASE)).isEqualTo(2);
        assertThat(RoundValidator.levelOf(11, BASE)).isEqualTo(2);
        assertThat(RoundValidator.levelOf(12, BASE)).isEqualTo(3);   // 5 + 6.25 = 11.25 이상
    }

    // ===== INVALID_TIME =====

    @Test
    @DisplayName("게임 시간이 실제 경과 시간보다 길면 INVALID_TIME")
    void gameTimeLongerThanServer() {
        assertFail(request(r -> {}), 1000, ErrorCode.INVALID_TIME);
    }

    @Test
    @DisplayName("CLEAR 인데 게임 시간이 챔피언 처치 시간과 다르면(처치 후 바로 보내지 않음) INVALID_TIME")
    void clearNotRightAfterChampion() {
        assertFail(request(r -> r.gameTime = 1250), SERVER_ELAPSED, ErrorCode.INVALID_TIME);
    }

    @Test
    @DisplayName("챔피언을 처치하지 않았는데 CLEAR 면 INVALID_TIME")
    void clearWithoutChampion() {
        assertFail(request(r -> r.bossTime = new BossTime(600.0, 600.0, 1200.0, null)),
                SERVER_ELAPSED, ErrorCode.INVALID_TIME);
    }

    // ===== INVALID_BOSS =====

    @Test
    @DisplayName("라이벌 출현 시간이 스케줄과 다르면 INVALID_BOSS")
    void rivalSpawnMismatch() {
        assertFail(request(r -> r.bossTime = new BossTime(500.0, 500.0, 1200.0, 1200.0)),
                SERVER_ELAPSED, ErrorCode.INVALID_BOSS);
    }

    @Test
    @DisplayName("보스 처치 시간이 출현 시간보다 앞서면 INVALID_BOSS")
    void killBeforeSpawn() {
        assertFail(request(r -> r.bossTime = new BossTime(600.0, 599.0, 1200.0, 1200.0)),
                SERVER_ELAPSED, ErrorCode.INVALID_BOSS);
    }

    @Test
    @DisplayName("보스전 동안 시계가 멈춰 처치 시간 = 출현 시간이어도 통과")
    void bossKillSameAsSpawn() {
        assertPass(request(r -> {}), SERVER_ELAPSED);   // 기본값이 600/600, 1200/1200
    }

    // ===== INVALID_POCKET_DOLLAR =====

    @Test
    @DisplayName("주운 포켓달러가 처치 수·보스 기준 최대치를 넘으면 INVALID_POCKET_DOLLAR")
    void tooMuchPocketDollar() {
        assertFail(request(r -> r.pocketDollar = 10_000_000), SERVER_ELAPSED, ErrorCode.INVALID_POCKET_DOLLAR);
    }

    @Test
    @DisplayName("출현 기록 없이 처치 기록만 있으면 INVALID_BOSS")
    void killWithoutSpawn() {
        assertFail(request(r -> r.bossTime = new BossTime(null, 600.0, 1200.0, 1200.0)),
                SERVER_ELAPSED, ErrorCode.INVALID_BOSS);
    }

    @Test
    @DisplayName("출현 시간을 넘겨 생존했는데 출현 기록이 없으면 INVALID_BOSS")
    void missingBossRecord() {
        assertFail(request(r -> {
            r.result = RoundResult.DEAD;
            r.gameTime = 900;
            r.bossTime = new BossTime(null, null, null, null);
        }), 950, ErrorCode.INVALID_BOSS);
    }

    // ===== INVALID_KILL / EXP / LEVEL / MONSTERBALL =====

    @Test
    @DisplayName("처치 수가 시간 대비 최대치를 넘으면 INVALID_KILL")
    void tooManyKills() {
        assertFail(request(r -> r.killCount = 40000), SERVER_ELAPSED, ErrorCode.INVALID_KILL);
    }

    @Test
    @DisplayName("경험치가 처치 수 대비 최대치를 넘으면 INVALID_EXP")
    void tooMuchExp() {
        assertFail(request(r -> r.totalExp = 10_000_000), SERVER_ELAPSED, ErrorCode.INVALID_EXP);
    }

    @Test
    @DisplayName("레벨이 누적 경험치와 맞지 않으면 INVALID_LEVEL")
    void levelMismatch() {
        assertFail(request(r -> r.level = 99), SERVER_ELAPSED, ErrorCode.INVALID_LEVEL);
    }

    @Test
    @DisplayName("재화 산식이 맞지 않으면 INVALID_MONSTERBALL")
    void ballFormulaMismatch() {
        assertFail(request(r -> r.ball = new MonsterBall(1200L, 800L, 100L, 9999L)),
                SERVER_ELAPSED, ErrorCode.INVALID_MONSTERBALL);
    }

    @Test
    @DisplayName("획득 재화가 최대치를 넘으면 INVALID_MONSTERBALL")
    void tooMuchBall() {
        assertFail(request(r -> r.ball = new MonsterBall(1_000_000L, 0L, 0L, 1_000_000L)),
                SERVER_ELAPSED, ErrorCode.INVALID_MONSTERBALL);
    }

    // ===== INVALID_ENTRY =====

    @Test
    @DisplayName("엔트리에 중복 포켓몬이 있으면 INVALID_ENTRY")
    void duplicateEntry() {
        assertFail(request(r -> r.entry = List.of(25, 25, 6)), SERVER_ELAPSED, ErrorCode.INVALID_ENTRY);
    }

    @Test
    @DisplayName("보유하지 않은 포켓몬이 있으면 INVALID_ENTRY")
    void notOwnedPokemon() {
        assertFail(request(r -> r.entry = List.of(25, 150)), SERVER_ELAPSED, ErrorCode.INVALID_ENTRY);
    }

    @Test
    @DisplayName("보유 포켓몬이 없으면(인트로 전) 어떤 엔트리도 INVALID_ENTRY")
    void noOwnedPokemon() {
        assertThatThrownBy(() -> validator.validate(request(r -> {}),
                new RoundValidationContext(SERVER_ELAPSED, RULE, BASE, Set.of(), KNOWN_TRAINERS, KNOWN_MONSTERS)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_ENTRY));
    }

    // ===== 해금 =====

    @Test
    @DisplayName("이번 라운드에서 해금한 포켓몬은 엔트리에 쓸 수 있다")
    void entryWithUnlockedPokemon() {
        assertPass(request(r -> {
            r.entry = List.of(25, 133);
            r.unlockedMonsters = List.of(133);
        }), SERVER_ELAPSED);
    }

    @Test
    @DisplayName("DEAD 여도 해금 목록은 검증을 통과한다")
    void unlockOnDead() {
        assertPass(request(r -> {
            r.result = RoundResult.DEAD;
            r.gameTime = 300;
            r.bossTime = new BossTime(null, null, null, null);
            r.unlockedTrainers = List.of(3);
            r.unlockedMonsters = List.of(133);
        }), 320);
    }

    @Test
    @DisplayName("마스터데이터에 없는 트레이너·포켓몬을 해금하면 INVALID_UNLOCK")
    void unlockUnknownId() {
        assertFail(request(r -> r.unlockedTrainers = List.of(999)), SERVER_ELAPSED, ErrorCode.INVALID_UNLOCK);
        assertFail(request(r -> r.unlockedMonsters = List.of(9999)), SERVER_ELAPSED, ErrorCode.INVALID_UNLOCK);
    }

    @Test
    @DisplayName("해금 목록에 중복이 있으면 INVALID_UNLOCK")
    void unlockDuplicate() {
        assertFail(request(r -> r.unlockedMonsters = List.of(133, 133)), SERVER_ELAPSED, ErrorCode.INVALID_UNLOCK);
    }

    @Test
    @DisplayName("결과 검증에 실패해도 해금 검증은 따로 통과할 수 있다 (해금은 유지)")
    void unlockValidIndependentOfResult() {
        RoundEndRequest req = request(r -> {
            r.killCount = 40000;                 // 결과 검증 실패 (INVALID_KILL)
            r.unlockedMonsters = List.of(133);   // 해금 자체는 정상
        });
        RoundValidationContext ctx = new RoundValidationContext(SERVER_ELAPSED, RULE, BASE, OWNED, KNOWN_TRAINERS, KNOWN_MONSTERS);
        assertThatCode(() -> validator.validateUnlocks(req, ctx)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateResult(req, ctx))
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_KILL));
    }

    @Test
    @DisplayName("검증 실패 예외는 로그용 reason 을 가진다")
    void failureHasReason() {
        assertThatThrownBy(() -> validate(request(r -> r.killCount = 40000), SERVER_ELAPSED))
                .isInstanceOf(RoundValidationException.class)
                .satisfies(e -> assertThat(((RoundValidationException) e).getReason()).contains("killCount 40000"));
    }

    // ===== 헬퍼 =====

    /** 기본값 = 정상 클리어 기록 */
    private static class Req {
        RoundResult result = RoundResult.CLEAR;
        double gameTime = 1200.0;   // 챔피언 처치 즉시 종료, 보스전 동안 시계 정지
        long totalExp = 52000;
        Integer level = null; // null 이면 totalExp 로 자동 계산
        MonsterBall ball = new MonsterBall(1200L, 800L, 100L, 500L);
        int killCount = 1820;
        List<Integer> entry = List.of(25, 6, 9);
        BossTime bossTime = new BossTime(600.0, 600.0, 1200.0, 1200.0);
        long pocketDollar = 1000;
        List<Integer> unlockedTrainers = List.of();
        List<Integer> unlockedMonsters = List.of();
    }

    private static RoundEndRequest request(Consumer<Req> modifier) {
        Req r = new Req();
        modifier.accept(r);
        int level = (r.level != null) ? r.level : RoundValidator.levelOf(r.totalExp, BASE);
        return new RoundEndRequest(r.result, r.gameTime, level, r.totalExp,
                r.ball, r.killCount, r.entry, r.bossTime, r.pocketDollar, r.unlockedTrainers, r.unlockedMonsters);
    }

    private void validate(RoundEndRequest req, double serverElapsed) {
        validator.validate(req, new RoundValidationContext(serverElapsed, RULE, BASE, OWNED, KNOWN_TRAINERS, KNOWN_MONSTERS));
    }

    private void assertPass(RoundEndRequest req, double serverElapsed) {
        assertThatCode(() -> validate(req, serverElapsed)).doesNotThrowAnyException();
    }

    private void assertFail(RoundEndRequest req, double serverElapsed, ErrorCode expected) {
        assertThatThrownBy(() -> validate(req, serverElapsed))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo(expected));
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }
}
