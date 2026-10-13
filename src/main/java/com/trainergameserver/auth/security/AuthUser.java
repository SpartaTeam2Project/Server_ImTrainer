package com.trainergameserver.auth.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 액세스 토큰의 사용자 이름(String)이 들어옵니다.
 * 토큰 검증은 AuthInterceptor가 이미 끝낸 상태입니다.
 * <pre>
 * public Foo bar(@AuthUser String username) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}
