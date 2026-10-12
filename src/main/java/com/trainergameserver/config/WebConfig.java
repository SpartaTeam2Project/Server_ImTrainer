package com.trainergameserver.config;

import com.trainergameserver.security.AuthInterceptor;
import com.trainergameserver.security.AuthUserArgumentResolver;
import com.trainergameserver.security.JwtProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 액세스 토큰 없이 호출할 수 있는 경로. 여기 없는 API는 모두 인증이 필요합니다. */
    private static final String[] PUBLIC_PATHS = {
            "/auth/signup",
            "/auth/login",
            "/auth/refresh",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtProvider jwtProvider;

    public WebConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(jwtProvider))
                .addPathPatterns("/**")
                .excludePathPatterns(PUBLIC_PATHS);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthUserArgumentResolver());
    }
}
