package com.trainergameserver;

import com.trainergameserver.player.service.CurrencyService;
import com.trainergameserver.player.service.PlayerConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** 재화 지급·차감. 다른 서비스(스테이지 결과 등)가 트랜잭션 안에서 호출하는 용도입니다. */
class CurrencyServiceTest extends ApiTestSupport {

    private static final String DOLLAR = CurrencyService.POCKET_DOLLAR;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private TransactionTemplate tx;

    @Test
    @DisplayName("트랜잭션 없이 호출하면 IllegalTransactionStateException")
    void requiresTransaction() throws Exception {
        Long userId = newUserId();
        assertThrows(IllegalTransactionStateException.class, () -> currencyService.grant(userId, DOLLAR, 100));
        assertThrows(IllegalTransactionStateException.class, () -> currencyService.spend(userId, DOLLAR, 100));
    }

    @Test
    @DisplayName("grant: 플레이데이터가 없던 계정도 지급되고, 변경 후 잔액을 돌려준다")
    void grant() throws Exception {
        String username = newUsername();
        signup(username);
        Long userId = userId(username);

        assertEquals(500, inTx(() -> currencyService.grant(userId, DOLLAR, 500)));
        assertEquals(800, inTx(() -> currencyService.grant(userId, DOLLAR, 300)));

        String token = login(username).str("accessToken");
        assertEquals(List.of(Map.of("currencyId", DOLLAR, "amount", 800)),
                post("/player/load", Map.of(), token).body().get("currencies"));
    }

    @Test
    @DisplayName("spend: 잔액이 부족하면 NOT_ENOUGH_CURRENCY, 잔액은 그대로")
    void spend() throws Exception {
        Long userId = newUserId();
        inTx(() -> currencyService.grant(userId, DOLLAR, 500));

        assertEquals(200, inTx(() -> currencyService.spend(userId, DOLLAR, 300)));
        PlayerConflictException e = assertThrows(PlayerConflictException.class,
                () -> inTx(() -> currencyService.spend(userId, DOLLAR, 201)));
        assertEquals("NOT_ENOUGH_CURRENCY", e.getCode().name());
        assertEquals(200L, currencyService.balance(userId, DOLLAR));
    }

    @Test
    @DisplayName("음수는 받지 않는다")
    void rejectsNegative() throws Exception {
        Long userId = newUserId();
        assertThrows(IllegalArgumentException.class, () -> inTx(() -> currencyService.grant(userId, DOLLAR, -1)));
        assertThrows(IllegalArgumentException.class, () -> inTx(() -> currencyService.spend(userId, DOLLAR, -1)));
    }

    /** 호출하는 쪽처럼 트랜잭션을 열고 실행 */
    private long inTx(LongSupplier call) {
        Long result = tx.execute(status -> call.getAsLong());
        return result == null ? 0 : result;
    }

    private Long newUserId() throws Exception {
        String username = newUsername();
        signup(username);
        return userId(username);
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }
}
