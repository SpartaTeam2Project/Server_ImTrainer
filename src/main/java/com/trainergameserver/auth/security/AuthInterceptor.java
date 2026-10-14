package com.trainergameserver.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 공개 API(WebConfig의 PUBLIC_PATHS)를 뺀 모든 컨트롤러 요청에서 액세스 토큰을 검증합니다.
 * 새 API를 만들 때 따로 인증 코드를 넣지 않아도 기본으로 보호됩니다.
 */
public class AuthInterceptor implements HandlerInterceptor {

    static final String USERNAME_ATTRIBUTE = AuthInterceptor.class.getName() + ".username";

    private final JwtProvider jwtProvider;

    public AuthInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 정적 리소스/404 처리 등 컨트롤러가 아닌 요청은 건너뜁니다.
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String username = jwtProvider.resolveAccessToken(request.getHeader("Authorization"));
        request.setAttribute(USERNAME_ATTRIBUTE, username);
        return true;
    }
}
