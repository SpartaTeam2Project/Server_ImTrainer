package com.trainergameserver;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API 테스트 공통 기능.
 * 실제로 서버를 랜덤 포트에 띄우고 HTTP로 호출합니다(클라이언트가 호출하는 것과 같은 방식).
 * 테스트 중 만든 사용자는 각 테스트가 끝나면 DB에서 지웁니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ApiTestSupport {

    protected static final String PASSWORD = "password123!";
    protected static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> createdUsers = new ArrayList<>();

    /** HTTP 상태 코드와 JSON 본문(본문이 없으면 빈 Map) */
    protected record Res(int status, Map<String, Object> body) {
        String str(String key) {
            return (String) body.get(key);
        }
    }

    protected Res post(String path, Object body) throws Exception {
        return post(path, body, null);
    }

    @SuppressWarnings("unchecked")
    protected Res post(String path, Object body, String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, Object> json = response.body().isBlank() ? Map.of() : JSON.readValue(response.body(), Map.class);
        return new Res(response.statusCode(), json);
    }

    /** 테스트마다 겹치지 않는 아이디 (끝나면 삭제 대상으로 기록) */
    protected String newUsername() {
        String username = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        createdUsers.add(username);
        return username;
    }

    protected Res signup(String username) throws Exception {
        return post("/auth/signup", Map.of("username", username, "password", PASSWORD));
    }

    protected Res login(String username) throws Exception {
        return post("/auth/login", Map.of("username", username, "password", PASSWORD));
    }

    /** 새 사용자로 가입 후 로그인한 응답 */
    protected Res newLoggedInUser() throws Exception {
        String username = newUsername();
        signup(username);
        return login(username);
    }

    @AfterEach
    void deleteCreatedUsers() {
        for (String username : createdUsers) {
            jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
        createdUsers.clear();
    }
}
