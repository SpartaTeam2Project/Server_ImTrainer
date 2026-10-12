package com.trainergameserver.service;

import com.trainergameserver.common.ConflictException;
import com.trainergameserver.common.UnauthorizedException;
import com.trainergameserver.dto.LoginRequest;
import com.trainergameserver.dto.SignupRequest;
import com.trainergameserver.dto.TokenResponse;
import com.trainergameserver.entity.RefreshToken;
import com.trainergameserver.entity.User;
import com.trainergameserver.repository.RefreshTokenRepository;
import com.trainergameserver.repository.UserRepository;
import com.trainergameserver.security.JwtProvider;
import com.trainergameserver.security.TokenHasher;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthService {

    private static final String DUPLICATE_USERNAME = "이미 사용 중인 아이디입니다.";
    private static final String LOGIN_FAILED = "아이디 또는 비밀번호가 일치하지 않습니다.";
    private static final String REFRESH_FAILED = "리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해 주세요.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    /** 없는 아이디로 로그인할 때도 BCrypt 비교를 한 번 수행해, 응답 시간으로 아이디 존재 여부를 알 수 없게 합니다. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtProvider jwtProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public void signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException(DUPLICATE_USERNAME);
        }
        try {
            userRepository.saveAndFlush(new User(
                    request.username(),
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 아이디로 가입 요청이 들어와 DB 유니크 제약에 걸린 경우만 409로 바꾸고, 그 밖의 무결성 오류는 그대로 던집니다.
            if (isUniqueViolation(e)) {
                throw new ConflictException(DUPLICATE_USERNAME);
            }
            throw e;
        }
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username()).orElse(null);
        String hash = (user != null) ? user.getPassword() : dummyPasswordHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new UnauthorizedException(LOGIN_FAILED);
        }
        return issueTokens(user);
    }

    /**
     * 리프레시 토큰을 검증하고, 사용한 토큰은 폐기한 뒤 새 토큰 쌍을 발급합니다(토큰 회전).
     * 폐기는 조건부 DELETE 한 번으로 처리해, 같은 토큰으로 동시에 요청해도 한 요청만 성공하고 나머지는 401을 받습니다.
     */
    public TokenResponse refresh(String refreshToken) {
        String username = jwtProvider.resolveRefreshToken(refreshToken);

        int deleted = refreshTokenRepository.deleteValid(TokenHasher.sha256(refreshToken), LocalDateTime.now());
        if (deleted == 0) {
            throw new UnauthorizedException(REFRESH_FAILED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(REFRESH_FAILED));
        return issueTokens(user);
    }

    /** 본인의 리프레시 토큰이면 폐기합니다. 이미 없으면 그대로 성공 처리합니다. */
    public void logout(String username, String refreshToken) {
        userRepository.findByUsername(username).ifPresent(user ->
                refreshTokenRepository.deleteByTokenHashAndUserId(TokenHasher.sha256(refreshToken), user.getId()));
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

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getUsername());
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());

        refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                TokenHasher.sha256(refreshToken),
                LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenSeconds())));

        return new TokenResponse(accessToken, refreshToken, jwtProvider.getAccessTokenSeconds());
    }
}
