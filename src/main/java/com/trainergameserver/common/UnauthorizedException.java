package com.trainergameserver.common;

/** 401. detail은 messages.properties의 error.UNAUTHORIZED.{detail} */
public class UnauthorizedException extends ApiException {

    public static final String LOGIN_FAILED = "login-failed";
    public static final String REFRESH_FAILED = "refresh-failed";
    public static final String NO_TOKEN = "no-token";
    public static final String INVALID_TOKEN = "invalid-token";

    public UnauthorizedException(String detail) {
        super(ErrorCode.UNAUTHORIZED, detail);
    }
}
