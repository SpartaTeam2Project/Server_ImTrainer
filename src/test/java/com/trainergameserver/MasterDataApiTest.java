package com.trainergameserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 클라이언트와 약속한 마스터데이터 해시 비교 규칙을 고정합니다. */
class MasterDataApiTest extends ApiTestSupport {

    @Test
    @DisplayName("액세스 토큰 없이 호출하면 401")
    void requiresAccessToken() throws Exception {
        assertEquals(401, post("/master-data", Map.of("hashes", Map.of())).status());
    }

    @Test
    @DisplayName("해시를 보내지 않으면 전체 테이블을 받고, hash는 data 문자열의 SHA-256이다")
    void returnsAllTablesWithoutHashes() throws Exception {
        String token = newLoggedInUser().str("accessToken");

        Map<String, Object> monster = monsterTable(post("/master-data", Map.of("hashes", Map.of()), token));
        String hash = (String) monster.get("hash");
        String data = (String) monster.get("data");

        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(sha256(data), hash, "클라는 받은 data로 해시를 다시 계산하므로 반드시 같아야 함");

        Map<?, ?> table = JSON.readValue(data, Map.class);
        assertFalse(((String) table.get("stageId")).isBlank());
        assertFalse(((List<?>) table.get("monsters")).isEmpty());

        // hashes 필드 자체가 없어도 전체를 받는다
        assertEquals(hash, monsterTable(post("/master-data", Map.of(), token)).get("hash"));
    }

    @Test
    @DisplayName("서버와 같은 해시를 보내면 바뀐 테이블이 없어 빈 tables를 받는다")
    void returnsNothingWhenHashMatches() throws Exception {
        String token = newLoggedInUser().str("accessToken");
        String hash = (String) monsterTable(post("/master-data", Map.of("hashes", Map.of()), token)).get("hash");

        Res res = post("/master-data", Map.of("hashes", Map.of("monster", hash)), token);
        assertEquals(200, res.status());
        assertEquals(Map.of(), res.body().get("tables"));
    }

    @Test
    @DisplayName("다른 해시를 보내면 그 테이블을 다시 받는다")
    void returnsTableWhenHashDiffers() throws Exception {
        String token = newLoggedInUser().str("accessToken");

        Res res = post("/master-data", Map.of("hashes", Map.of("monster", "old-hash")), token);
        assertNotNull(monsterTable(res).get("data"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> monsterTable(Res res) {
        assertEquals(200, res.status());
        Map<String, Object> tables = (Map<String, Object>) res.body().get("tables");
        assertNotNull(tables.get("monster"), "monster 테이블이 응답에 있어야 함");
        return (Map<String, Object>) tables.get("monster");
    }

    private static String sha256(String text) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
