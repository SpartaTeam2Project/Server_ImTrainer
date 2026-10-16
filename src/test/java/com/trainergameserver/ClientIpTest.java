package com.trainergameserver;

import com.trainergameserver.auth.security.LoginAttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 프록시(CloudFront 등) 뒤에서 X-Forwarded-For의 마지막 값을 클라이언트 IP로 쓰는지 (로그인 시도 제한 기준) */
@TestPropertySource(properties = {
        "client-ip.header=X-Forwarded-For",
        "login.max-failures-per-account=2"
})
class ClientIpTest extends ApiTestSupport {

    @Autowired
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void reset() {
        limiter.clear();
    }

    @Test
    @DisplayName("X-Forwarded-For의 마지막 IP별로 따로 센다")
    void countsPerForwardedIp() throws Exception {
        String username = newUsername();
        signup(username);

        wrongLogin(username, "1.1.1.1");
        wrongLogin(username, "1.1.1.1");
        assertEquals(429, wrongLogin(username, "1.1.1.1"));   // 1.1.1.1은 차단
        assertEquals(401, wrongLogin(username, "2.2.2.2"));   // 다른 사용자(IP)는 영향 없음
    }

    @Test
    @DisplayName("앞쪽 값을 위조해도 프록시가 붙인 마지막 IP 기준으로 센다")
    void ignoresSpoofedPrefix() throws Exception {
        String username = newUsername();
        signup(username);

        wrongLogin(username, "9.9.9.9, 1.1.1.1");
        wrongLogin(username, "8.8.8.8, 1.1.1.1");
        assertEquals(429, wrongLogin(username, "7.7.7.7, 1.1.1.1"));
    }

    private int wrongLogin(String username, String forwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
