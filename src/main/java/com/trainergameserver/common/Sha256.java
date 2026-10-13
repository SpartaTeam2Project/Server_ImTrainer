package com.trainergameserver.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 소문자 hex. 리프레시 토큰 저장(토큰 자체가 충분히 무작위라 salt 불필요)과 마스터데이터 해시에 씁니다. */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
