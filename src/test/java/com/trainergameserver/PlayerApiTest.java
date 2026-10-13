package com.trainergameserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 플레이데이터·구매 검증 API. 클라가 계산한 값을 서버가 다시 계산해 맞으면 저장, 다르면 409 + 서버 기준 playerData.
 * 기준 마스터데이터: training.json (Attack id 6: 260/650/1500/3300/7200, QuickClaw id 9: 공격 카테고리 레벨 1 필요),
 * trainer.json (1세대 남자 → 레드 id 1, 1세대 여자 → 리프 id 2), storage_monster.json (이상해씨 id 1, id 2는 없음)
 */
class PlayerApiTest extends ApiTestSupport {

    private static final int ATTACK = 6;
    private static final int QUICK_CLAW = 9;

    @Test
    @DisplayName("신규 계정은 인트로 전, 빈 목록, pocket_dollar 0, 닉네임은 아이디")
    void loadNewPlayer() throws Exception {
        String username = newUsername();
        signup(username);
        String token = login(username).str("accessToken");

        Res res = post("/player/load", Map.of(), token);
        assertEquals(200, res.status());
        assertEquals(username, res.str("nickname"));
        assertEquals(Map.of("done", false, "gender", 0, "generation", 0, "starterMonsterId", 0), res.body().get("intro"));
        assertEquals(0, res.body().get("selectedTrainerId"));
        assertEquals(List.of(), res.body().get("trainings"));
        assertEquals(List.of(Map.of("currencyId", "pocket_dollar", "amount", 0)), res.body().get("currencies"));
        assertEquals(List.of(), res.body().get("ownedTrainerIds"));
        assertEquals(List.of(), res.body().get("ownedMonsterIds"));
    }

    @Test
    @DisplayName("액세스 토큰 없이 호출하면 401")
    void requiresAccessToken() throws Exception {
        assertEquals(401, post("/player/load", Map.of()).status());
    }

    @Test
    @DisplayName("인트로: 스타터 포켓몬과 세대·성별에 맞는 트레이너를 받고 선택된다. 두 번은 못 한다")
    void intro() throws Exception {
        String token = newLoggedInUser().str("accessToken");

        Res res = post("/player/intro", intro("레드", 0, 1, 1, 1), token);
        assertEquals(200, res.status());
        assertEquals("레드", res.str("nickname"));
        assertEquals(Map.of("done", true, "gender", 0, "generation", 1, "starterMonsterId", 1), res.body().get("intro"));
        assertEquals(1, res.body().get("selectedTrainerId"));
        assertEquals(List.of(1), res.body().get("ownedTrainerIds"));
        assertEquals(List.of(1), res.body().get("ownedMonsterIds"));

        Res again = post("/player/intro", intro("레드", 0, 1, 4, 1), token);
        assertConflict(again, "INTRO_ALREADY_DONE");
        assertEquals(List.of(1), playerData(again).get("ownedMonsterIds"));
    }

    @Test
    @DisplayName("인트로: 세대·성별에 맞지 않는 트레이너, 없는 포켓몬이면 409와 인트로 전 상태를 돌려준다")
    void introRejected() throws Exception {
        String token = newLoggedInUser().str("accessToken");

        assertConflict(post("/player/intro", intro("리프", 0, 1, 1, 2), token), "REQUIREMENT_NOT_MET"); // 남자인데 리프
        assertConflict(post("/player/intro", intro("심향", 0, 2, 1, 7), token), "REQUIREMENT_NOT_MET"); // 2세대는 인트로 지급 트레이너 없음
        Res unknownMonster = post("/player/intro", intro("레드", 0, 1, 2, 1), token);
        assertConflict(unknownMonster, "UNKNOWN_ID");
        assertEquals(false, ((Map<?, ?>) playerData(unknownMonster).get("intro")).get("done"));
    }

    @Test
    @DisplayName("인트로: 닉네임이 비어 있으면 400")
    void introValidation() throws Exception {
        String token = newLoggedInUser().str("accessToken");
        Res res = post("/player/intro", intro(" ", 0, 1, 1, 1), token);
        assertEquals(400, res.status());
        assertEquals("INVALID_REQUEST", res.str("code"));
    }

    @Test
    @DisplayName("트레이닝 구매: 맞게 계산하면 레벨이 오르고 잔액이 줄어든다")
    void purchase() throws Exception {
        String username = newUsername();
        String token = loggedInWithDollar(username, 1000);

        Res res = purchase(token, ATTACK, 1, 260, 740);
        assertEquals(200, res.status());
        assertEquals(List.of(Map.of("trainingId", ATTACK, "level", 1)), res.body().get("trainings"));
        assertEquals(List.of(Map.of("currencyId", "pocket_dollar", "amount", 740)), res.body().get("currencies"));

        // 저장되었는지 다시 조회
        assertEquals(res.body(), post("/player/load", Map.of(), token).body());
    }

    @Test
    @DisplayName("트레이닝 구매: 잔액 부족, 비용·잔액·레벨 불일치, 해금 전이면 409와 바뀌지 않은 playerData")
    void purchaseRejected() throws Exception {
        String poorToken = newLoggedInUser().str("accessToken");
        assertConflict(purchase(poorToken, ATTACK, 1, 260, -260), "NOT_ENOUGH_CURRENCY");

        String token = loggedInWithDollar(newUsername(), 1000);
        assertConflict(purchase(token, ATTACK, 1, 100, 900), "MISMATCH");   // 비용
        assertConflict(purchase(token, ATTACK, 1, 260, 800), "MISMATCH");   // 구매 후 잔액
        assertConflict(purchase(token, ATTACK, 2, 650, 350), "MISMATCH");   // 레벨 건너뛰기
        Res locked = purchase(token, QUICK_CLAW, 1, 260, 740);               // 공격 카테고리 레벨 1 필요
        assertConflict(locked, "REQUIREMENT_NOT_MET");
        assertConflict(purchase(token, 7, 1, 260, 740), "UNKNOWN_ID");       // id 7은 삭제된 트레이닝

        Map<?, ?> unchanged = playerData(locked);
        assertEquals(List.of(), unchanged.get("trainings"));
        assertEquals(List.of(Map.of("currencyId", "pocket_dollar", "amount", 1000)), unchanged.get("currencies"));

        // 공격 훈련을 사면 선제공격손톱이 해금된다
        assertEquals(200, purchase(token, ATTACK, 1, 260, 740).status());
        assertEquals(200, purchase(token, QUICK_CLAW, 1, 260, 480).status());
    }

    @Test
    @DisplayName("트레이닝 구매: 최대 레벨이면 MAX_LEVEL")
    void purchaseMaxLevel() throws Exception {
        String token = loggedInWithDollar(newUsername(), 20000);
        long balance = 20000;
        int[] costs = {260, 650, 1500, 3300, 7200};
        for (int level = 1; level <= costs.length; level++) {
            balance -= costs[level - 1];
            assertEquals(200, purchase(token, ATTACK, level, costs[level - 1], balance).status());
        }
        assertConflict(purchase(token, ATTACK, 6, 0, balance), "MAX_LEVEL");
    }

    @Test
    @DisplayName("트레이닝 초기화: 산 비용을 전액 환불하고 레벨을 모두 0으로. 환불액이 다르면 MISMATCH")
    void reset() throws Exception {
        String token = loggedInWithDollar(newUsername(), 1000);
        purchase(token, ATTACK, 1, 260, 740);
        purchase(token, ATTACK, 2, 650, 90);
        purchase(token, QUICK_CLAW, 1, 260, -170); // 잔액 부족으로 실패 (영향 없음)

        assertConflict(post("/training/reset", Map.of("refund", 260, "balanceAfter", 350), token), "MISMATCH");

        Res res = post("/training/reset", Map.of("refund", 910, "balanceAfter", 1000), token);
        assertEquals(200, res.status());
        assertEquals(List.of(), res.body().get("trainings"));
        assertEquals(List.of(Map.of("currencyId", "pocket_dollar", "amount", 1000)), res.body().get("currencies"));
    }

    @Test
    @DisplayName("트레이너 선택: 보유한 트레이너만 선택할 수 있다")
    void selectTrainer() throws Exception {
        String token = newLoggedInUser().str("accessToken");
        post("/player/intro", intro("레드", 0, 1, 1, 1), token);

        assertConflict(post("/player/trainer/select", Map.of("trainerId", 3), token), "NOT_OWNED");
        Res res = post("/player/trainer/select", Map.of("trainerId", 1), token);
        assertEquals(200, res.status());
        assertEquals(1, res.body().get("selectedTrainerId"));
    }

    @Test
    @DisplayName("필수 값이 없으면 400 INVALID_REQUEST")
    void validation() throws Exception {
        String token = newLoggedInUser().str("accessToken");
        Res res = post("/training/purchase", Map.of("trainingId", ATTACK), token);
        assertEquals(400, res.status());
        assertEquals("INVALID_REQUEST", res.str("code"));
    }

    private static Map<String, Object> intro(String nickname, int gender, int generation, int starterMonsterId, int trainerId) {
        return Map.of("nickname", nickname, "gender", gender, "generation", generation,
                "starterMonsterId", starterMonsterId, "trainerId", trainerId);
    }

    private Res purchase(String token, int trainingId, int toLevel, long cost, long balanceAfter) throws Exception {
        return post("/training/purchase", Map.of("trainingId", trainingId, "toLevel", toLevel, "cost", cost,
                "currencyId", "pocket_dollar", "balanceAfter", balanceAfter), token);
    }

    /** 가입·로그인 후 pocket_dollar를 지급한 계정의 액세스 토큰 (재화 획득 API가 아직 없어 DB에 직접 넣음) */
    private String loggedInWithDollar(String username, long amount) throws Exception {
        signup(username);
        String token = login(username).str("accessToken");
        post("/player/load", Map.of(), token); // players 행 생성
        jdbcTemplate.update("""
                INSERT INTO player_currencies (user_id, currency_id, amount)
                SELECT id, 'pocket_dollar', ? FROM users WHERE username = ?""", amount, username);
        return token;
    }

    private static void assertConflict(Res res, String code) {
        assertEquals(409, res.status(), () -> "409가 아님: " + res.body());
        assertEquals(code, res.str("code"));
        assertNotNull(res.body().get("playerData"), "409에는 서버 기준 playerData가 있어야 함");
    }

    private static Map<?, ?> playerData(Res res) {
        return (Map<?, ?>) res.body().get("playerData");
    }
}
