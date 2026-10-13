package com.trainergameserver.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 리프레시 토큰을 DB에 저장/조회할 때 쓰는 SHA-256 해시. 토큰 자체가 충분히 무작위라 salt는 필요 없습니다. */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String sha256(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
