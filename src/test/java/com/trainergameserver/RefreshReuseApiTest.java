package com.trainergameserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 리프레시 토큰 재사용 감지. 유예 시간을 0으로 두어 재사용을 바로 탈취로 판정하게 합니다. */
@TestPropertySource(properties = "jwt.refresh-reuse-grace-seconds=0")
class RefreshReuseApiTest extends ApiTestSupport {

    @Test
    @DisplayName("이미 사용한 리프레시 토큰이 다시 오면 그 로그인에서 발급된 토큰이 모두 폐기된다")
    void reuseRevokesFamily() throws Exception {
        Res login = newLoggedInUser();
        String first = login.str("refreshToken");

        Res refreshed = post("/auth/refresh", Map.of("refreshToken", first));
        assertEquals(200, refreshed.status());
        String second = refreshed.str("refreshToken");

        // 탈취된 토큰(first)을 누군가 다시 사용
        assertEquals(401, post("/auth/refresh", Map.of("refreshToken", first)).status());

        // 정상 사용자가 가진 최신 토큰(second)도 폐기되어 다시 로그인해야 한다
        Res afterReuse = post("/auth/refresh", Map.of("refreshToken", second));
        assertEquals(401, afterReuse.status());
        assertEquals("UNAUTHORIZED", afterReuse.str("code"));
    }

    @Test
    @DisplayName("다른 로그인(기기)의 토큰은 재사용 감지의 영향을 받지 않는다")
    void otherSessionsSurvive() throws Exception {
        String username = newUsername();
        signup(username);
        String deviceA = login(username).str("refreshToken");
        String deviceB = login(username).str("refreshToken");

        post("/auth/refresh", Map.of("refreshToken", deviceA));
        post("/auth/refresh", Map.of("refreshToken", deviceA)); // A 쪽 재사용 → A family 폐기

        assertEquals(200, post("/auth/refresh", Map.of("refreshToken", deviceB)).status());
    }
}
