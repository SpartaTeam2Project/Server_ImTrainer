package com.trainergameserver;

import com.trainergameserver.auth.security.LoginAttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 로그인 시도 제한. 테스트 요청은 모두 같은 IP(127.0.0.1)에서 오므로 테스트마다 기록을 비웁니다. */
@TestPropertySource(properties = {
        "login.max-failures-per-account=5",
        "login.max-failures-per-ip=8"
})
class LoginLimitApiTest extends ApiTestSupport {

    @Autowired
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void resetLimiter() {
        limiter.clear();
    }

    @Test
    @DisplayName("같은 아이디로 5번 실패하면 비밀번호가 맞아도 429와 Retry-After를 받는다")
    void blocksAccountAfterFailures() throws Exception {
        String username = newUsername();
        signup(username);

        for (int i = 0; i < 5; i++) {
            assertEquals(401, wrongPassword(username).status());
        }

        Res blocked = login(username);
        assertEquals(429, blocked.status());
        assertEquals("TOO_MANY_REQUESTS", blocked.str("code"));
        assertTrue(Long.parseLong(blocked.header("Retry-After")) > 0);
    }

    @Test
    @DisplayName("성공하면 그 아이디의 실패 횟수가 초기화된다")
    void successResetsAccountCount() throws Exception {
        String username = newUsername();
        signup(username);

        for (int i = 0; i < 4; i++) {
            wrongPassword(username);
        }
        assertEquals(200, login(username).status());
        // 초기화되지 않았다면 4 + 3 = 7번째 실패에서 이미 차단됐어야 함 (IP 한도 8은 넘지 않게 3번만)
        for (int i = 0; i < 3; i++) {
            assertEquals(401, wrongPassword(username).status());
        }
        assertEquals(200, login(username).status());
    }

    @Test
    @DisplayName("같은 IP에서 아이디를 바꿔 가며 실패해도 한도를 넘으면 429")
    void blocksIpAfterFailures() throws Exception {
        for (int i = 0; i < 8; i++) {
            assertEquals(401, wrongPassword(newUsername()).status());
        }
        String username = newUsername();
        signup(username);
        assertEquals(429, login(username).status());
    }

    @Test
    @DisplayName("프록시 설정이 없으면 X-Forwarded-For를 위조해도 접속 IP 기준으로 센다")
    void ignoresForwardedHeaderByDefault() throws Exception {
        String username = newUsername();
        signup(username);
        for (int i = 0; i < 5; i++) {
            java.net.http.HttpRequest request = java.net.http.HttpRequest
                    .newBuilder(java.net.URI.create("http://localhost:" + port + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .header("X-Forwarded-For", "10.0.0." + i)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                            "{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                    .build();
            HTTP.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        }
        assertEquals(429, login(username).status());
    }

    private Res wrongPassword(String username) throws Exception {
        return post("/auth/login", Map.of("username", username, "password", "wrong-password"));
    }
}
