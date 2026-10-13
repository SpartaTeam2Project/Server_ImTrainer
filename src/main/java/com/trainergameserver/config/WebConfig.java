package com.trainergameserver.config;

import com.trainergameserver.auth.security.AuthInterceptor;
import com.trainergameserver.auth.security.AuthUserArgumentResolver;
import com.trainergameserver.auth.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 액세스 토큰 없이 호출할 수 있는 경로. 여기 없는 API는 모두 인증이 필요합니다. */
    private static final String[] PUBLIC_PATHS = {
            "/auth/signup",
            "/auth/login",
            "/auth/refresh",
            "/error",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtProvider jwtProvider;

    /** 웹 빌드가 호출할 출처(예: https://game.example.com). 비어 있으면 CORS를 허용하지 않습니다. */
    private final List<String> allowedOrigins;

    public WebConfig(JwtProvider jwtProvider,
                     @Value("${cors.allowed-origins:}") String allowedOrigins) {
        this.jwtProvider = jwtProvider;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
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

    /**
     * 브라우저(WebGL 빌드 등)에서 다른 출처로 API를 호출할 수 있게 합니다.
     * 토큰은 쿠키가 아니라 Authorization 헤더로 보내므로 credentials는 허용하지 않습니다.
     * Unity 네이티브 빌드(PC, 모바일)는 CORS와 무관합니다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .exposedHeaders("Retry-After")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
