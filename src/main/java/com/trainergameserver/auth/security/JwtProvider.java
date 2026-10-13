package com.trainergameserver.auth.security;

import com.trainergameserver.common.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 발급/검증. 리프레시 토큰의 저장/폐기는 AuthService가 DB로 관리합니다.
 */
@Component
public class JwtProvider {

    private static final String TYPE = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-seconds}") long accessTokenSeconds,
                       @Value("${jwt.refresh-token-seconds}") long refreshTokenSeconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public String createAccessToken(String username) {
        return create(username, ACCESS, accessTokenSeconds);
    }

    public String createRefreshToken(String username) {
        return create(username, REFRESH, refreshTokenSeconds);
    }

    public long getAccessTokenSeconds() {
        return accessTokenSeconds;
    }

    public long getRefreshTokenSeconds() {
        return refreshTokenSeconds;
    }

    /** Authorization 헤더에서 액세스 토큰을 꺼내 검증하고 사용자 이름을 돌려줍니다. */
    public String resolveAccessToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException(UnauthorizedException.NO_TOKEN);
        }
        return parse(authorization.substring(7), ACCESS, UnauthorizedException.INVALID_TOKEN);
    }

    /** 리프레시 토큰을 검증하고 사용자 이름을 돌려줍니다. */
    public String resolveRefreshToken(String refreshToken) {
        return parse(refreshToken, REFRESH, UnauthorizedException.REFRESH_FAILED);
    }

    private String create(String username, String type, long seconds) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // 같은 초에 발급돼도 토큰이 겹치지 않도록
                .subject(username)
                .claim(TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + seconds * 1000))
                .signWith(key)
                .compact();
    }

    private String parse(String token, String expectedType, String failureDetail) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get(TYPE, String.class))) {
                throw new UnauthorizedException(failureDetail);
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(failureDetail);
        }
    }
}
