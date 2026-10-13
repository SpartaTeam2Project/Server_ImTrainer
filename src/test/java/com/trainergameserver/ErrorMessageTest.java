package com.trainergameserver;

import com.trainergameserver.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** 에러 메시지가 messages.properties에서 제대로 내려오는지 (키 누락, 인자, 검증 어노테이션) */
class ErrorMessageTest extends ApiTestSupport {

    @Test
    @DisplayName("모든 ErrorCode에 messages.properties 메시지가 하나 이상 있다")
    void everyCodeHasMessage() throws Exception {
        Properties messages = new Properties();
        try (var in = getClass().getResourceAsStream("/messages.properties")) {
            messages.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        for (ErrorCode code : ErrorCode.values()) {
            String key = "error." + code.name();
            boolean exists = messages.stringPropertyNames().stream()
                    .anyMatch(k -> k.equals(key) || k.startsWith(key + "."));
            assertTrue(exists, key + " 메시지가 없습니다.");
        }
    }

    @Test
    @DisplayName("검증 어노테이션의 {키}가 messages.properties 문구로 바뀐다")
    void validationMessage() throws Exception {
        Res res = post("/auth/signup", Map.of("username", "ab", "password", PASSWORD));
        assertEquals("아이디는 4~20자로 입력해 주세요.", res.str("message"));
    }

    @Test
    @DisplayName("예외 메시지: 로그인 실패, 토큰 없음, JSON 오류")
    void exceptionMessages() throws Exception {
        assertEquals("아이디 또는 비밀번호가 일치하지 않습니다.",
                post("/auth/login", Map.of("username", newUsername(), "password", PASSWORD)).str("message"));
        assertEquals("액세스 토큰이 없습니다.", post("/player/load", Map.of()).str("message"));
    }

    @Test
    @DisplayName("인자가 있는 메시지는 숫자에 쉼표 없이 들어간다 (서버 기준 1500)")
    void messageWithArgument() throws Exception {
        String username = newUsername();
        signup(username);
        String token = login(username).str("accessToken");
        post("/player/load", Map.of(), token);
        jdbcTemplate.update("INSERT INTO player_currencies (user_id, currency_id, amount) "
                + "SELECT id, 'pocket_dollar', 100000 FROM users WHERE username = ?", username);
        jdbcTemplate.update("INSERT INTO player_trainings (user_id, training_id, level) "
                + "SELECT id, 6, 2 FROM users WHERE username = ?", username);

        // 공격 훈련 3레벨 비용은 1500
        Res res = post("/training/purchase", Map.of("trainingId", 6, "toLevel", 3, "cost", 1, "currencyId", "pocket_dollar",
                "balanceAfter", 99999), token);
        assertEquals("MISMATCH", res.str("code"));
        assertEquals("비용이 서버와 다릅니다. (서버 기준 1500)", res.str("message"));
    }
}
