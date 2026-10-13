package com.trainergameserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 클라이언트가 쓰는 인증 API의 동작을 고정합니다. 머지 후 이 테스트가 깨지면 클라이언트도 깨집니다. */
class AuthApiTest extends ApiTestSupport {

    @Test
    @DisplayName("회원가입 후 로그인하면 액세스/리프레시 토큰과 만료 시간(1800초)을 받는다")
    void signupAndLogin() throws Exception {
        String username = newUsername();

        assertEquals(201, signup(username).status());

        Res login = login(username);
        assertEquals(200, login.status());
        assertFalse(login.str("accessToken").isBlank());
        assertFalse(login.str("refreshToken").isBlank());
        assertEquals(1800, ((Number) login.body().get("expiresIn")).intValue());
    }

    @Test
    @DisplayName("같은 아이디로 다시 가입하면 409 DUPLICATE_USERNAME")
    void duplicateSignup() throws Exception {
        String username = newUsername();
        signup(username);

        Res again = signup(username);
        assertEquals(409, again.status());
        assertEquals("DUPLICATE_USERNAME", again.str("code"));
    }

    @Test
    @DisplayName("입력값이 형식에 맞지 않으면 400 INVALID_REQUEST")
    void invalidSignup() throws Exception {
        Res res = post("/auth/signup", Map.of("username", "ab", "password", PASSWORD));
        assertEquals(400, res.status());
        assertEquals("INVALID_REQUEST", res.str("code"));
    }

    @Test
    @DisplayName("비밀번호가 틀려도, 없는 아이디여도 같은 401 응답 (어느 쪽이 틀렸는지 알려주지 않음)")
    void loginFailure() throws Exception {
        String username = newUsername();
        signup(username);

        Res wrongPassword = post("/auth/login", Map.of("username", username, "password", "wrong-password"));
        Res unknownUser = post("/auth/login", Map.of("username", newUsername(), "password", PASSWORD));

        assertEquals(401, wrongPassword.status());
        assertEquals("UNAUTHORIZED", wrongPassword.str("code"));
        assertEquals(wrongPassword.body(), unknownUser.body());
        assertEquals(401, unknownUser.status());
    }

    @Test
    @DisplayName("리프레시하면 새 토큰 쌍을 받고, 사용한 리프레시 토큰은 다시 쓸 수 없다")
    void refreshRotation() throws Exception {
        Res login = newLoggedInUser();
        String oldRefresh = login.str("refreshToken");

        Res refreshed = post("/auth/refresh", Map.of("refreshToken", oldRefresh));
        assertEquals(200, refreshed.status());
        assertNotEquals(oldRefresh, refreshed.str("refreshToken"));
        assertNotEquals(login.str("accessToken"), refreshed.str("accessToken"));

        Res reused = post("/auth/refresh", Map.of("refreshToken", oldRefresh));
        assertEquals(401, reused.status());
        assertEquals("UNAUTHORIZED", reused.str("code"));

        // 유예 시간(10초) 안의 재사용은 동시 요청으로 보고, 새로 받은 토큰은 그대로 쓸 수 있다
        assertEquals(200, post("/auth/refresh", Map.of("refreshToken", refreshed.str("refreshToken"))).status());
    }

    @Test
    @DisplayName("로그아웃은 액세스 토큰이 필요하고, 로그아웃한 리프레시 토큰은 다시 쓸 수 없다")
    void logout() throws Exception {
        Res login = newLoggedInUser();
        Map<String, String> body = Map.of("refreshToken", login.str("refreshToken"));

        assertEquals(401, post("/auth/logout", body).status());
        assertEquals(204, post("/auth/logout", body, login.str("accessToken")).status());
        assertEquals(401, post("/auth/refresh", body).status());
    }
}
