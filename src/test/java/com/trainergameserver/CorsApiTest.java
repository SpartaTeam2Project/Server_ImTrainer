package com.trainergameserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/** 웹 빌드용 CORS. 허용한 출처의 preflight만 통과해야 합니다. */
@TestPropertySource(properties = "cors.allowed-origins=https://game.example.com")
class CorsApiTest extends ApiTestSupport {

    @Test
    @DisplayName("허용한 출처의 preflight는 200과 Access-Control-Allow-Origin을 받는다 (액세스 토큰 불필요)")
    void allowedOrigin() throws Exception {
        HttpResponse<String> res = preflight("https://game.example.com");
        assertEquals(200, res.statusCode());
        assertEquals("https://game.example.com",
                res.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    @Test
    @DisplayName("허용하지 않은 출처의 preflight는 403")
    void disallowedOrigin() throws Exception {
        HttpResponse<String> res = preflight("https://evil.example.com");
        assertEquals(403, res.statusCode());
        assertTrue(res.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
    }

    private HttpResponse<String> preflight(String origin) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/master-data"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
