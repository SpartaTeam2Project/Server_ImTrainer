package com.trainergameserver.auth.service;

import com.trainergameserver.auth.dto.LoginRequest;
import com.trainergameserver.auth.dto.SignupRequest;
import com.trainergameserver.auth.dto.TokenResponse;
import com.trainergameserver.auth.entity.RefreshToken;
import com.trainergameserver.auth.entity.User;
import com.trainergameserver.auth.repository.RefreshTokenRepository;
import com.trainergameserver.auth.repository.UserRepository;
import com.trainergameserver.auth.security.JwtProvider;
import com.trainergameserver.auth.security.LoginAttemptLimiter;
import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;
import com.trainergameserver.common.Sha256;
import com.trainergameserver.common.UnauthorizedException;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final long refreshReuseGraceSeconds;

    /** 없는 아이디로 로그인할 때도 BCrypt 비교를 한 번 수행해, 응답 시간으로 아이디 존재 여부를 알 수 없게 합니다. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtProvider jwtProvider,
                       PasswordEncoder passwordEncoder,
                       LoginAttemptLimiter loginAttemptLimiter,
                       @Value("${jwt.refresh-reuse-grace-seconds}") long refreshReuseGraceSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.refreshReuseGraceSeconds = refreshReuseGraceSeconds;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public void signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(ErrorCode.DUPLICATE_USERNAME);
        }
        try {
            userRepository.saveAndFlush(new User(
                    request.username(),
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 아이디로 가입 요청이 들어와 DB 유니크 제약에 걸린 경우만 409로 바꾸고, 그 밖의 무결성 오류는 그대로 던집니다.
            if (isUniqueViolation(e)) {
                throw new ApiException(ErrorCode.DUPLICATE_USERNAME);
            }
            throw e;
        }
    }

    /** 실패가 반복되면 LoginAttemptLimiter가 비밀번호 비교 전에 429로 막습니다. */
    public TokenResponse login(LoginRequest request, String clientIp) {
        loginAttemptLimiter.checkAllowed(clientIp, request.username());

        User user = userRepository.findByUsername(request.username()).orElse(null);
        String hash = (user != null) ? user.getPassword() : dummyPasswordHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            loginAttemptLimiter.recordFailure(clientIp, request.username());
            throw new UnauthorizedException(UnauthorizedException.LOGIN_FAILED);
        }
        loginAttemptLimiter.recordSuccess(clientIp, request.username());
        return issueTokens(user, UUID.randomUUID().toString());
    }

    /**
     * 리프레시 토큰을 검증하고, 사용 처리한 뒤 같은 family로 새 토큰 쌍을 발급합니다(토큰 회전).
     * <ul>
     *   <li>사용 처리는 조건부 UPDATE 한 번이라, 같은 토큰으로 동시에 요청해도 한 요청만 성공합니다.</li>
     *   <li>이미 사용한 토큰이 유예 시간(jwt.refresh-reuse-grace-seconds) 안에 다시 오면 동시 요청으로 보고 401만 반환합니다.</li>
     *   <li>유예 시간이 지난 뒤 다시 오면 토큰이 탈취된 것으로 보고 같은 family의 토큰을 모두 폐기합니다.
     *       공격자와 사용자 모두 다시 로그인해야 합니다.</li>
     * </ul>
     * 401을 던져도 family 폐기는 커밋되어야 하므로 UnauthorizedException에서는 롤백하지 않습니다.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(String refreshToken) {
        jwtProvider.resolveRefreshToken(refreshToken); // 서명, 만료, 타입 검증
        String tokenHash = Sha256.hex(refreshToken);
        LocalDateTime now = LocalDateTime.now();

        if (refreshTokenRepository.markUsed(tokenHash, now) == 1) {
            RefreshToken used = refreshTokenRepository.findByTokenHash(tokenHash)
                    .orElseThrow(() -> new UnauthorizedException(UnauthorizedException.REFRESH_FAILED));
            User user = userRepository.findById(used.getUserId())
                    .orElseThrow(() -> new UnauthorizedException(UnauthorizedException.REFRESH_FAILED));
            return issueTokens(user, used.getFamilyId());
        }

        refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> token.getUsedAt() != null)
                .filter(token -> !token.getUsedAt().isAfter(now.minusSeconds(refreshReuseGraceSeconds)))
                .ifPresent(token -> {
                    int revoked = refreshTokenRepository.deleteByFamilyId(token.getFamilyId());
                    log.warn("리프레시 토큰 재사용 감지: userId={}, family={}, 폐기 {}건",
                            token.getUserId(), token.getFamilyId(), revoked);
                });
        throw new UnauthorizedException(UnauthorizedException.REFRESH_FAILED);
    }

    /** 본인의 리프레시 토큰이면 그 로그인에서 나온 토큰(family)을 모두 폐기합니다. 이미 없으면 그대로 성공 처리합니다. */
    public void logout(String username, String refreshToken) {
        userRepository.findByUsername(username).ifPresent(user ->
                refreshTokenRepository.findByTokenHash(Sha256.hex(refreshToken))
                        .filter(token -> token.getUserId().equals(user.getId()))
                        .ifPresent(token -> refreshTokenRepository.deleteByFamilyId(token.getFamilyId())));
    }

    private static boolean isUniqueViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                return true;
            }
        }
        return false;
    }

    private TokenResponse issueTokens(User user, String familyId) {
        String accessToken = jwtProvider.createAccessToken(user.getUsername());
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());

        refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                Sha256.hex(refreshToken),
                familyId,
                LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenSeconds())));

        return new TokenResponse(accessToken, refreshToken, jwtProvider.getAccessTokenSeconds());
    }
}
