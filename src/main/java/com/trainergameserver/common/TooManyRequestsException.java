package com.trainergameserver.common;

/** 429. 응답에 Retry-After 헤더(초)가 붙습니다. */
public class TooManyRequestsException extends ApiException {

    private final long retryAfterSeconds;

    /** detail은 messages.properties의 error.TOO_MANY_REQUESTS.{detail} */
    public TooManyRequestsException(String detail, long retryAfterSeconds, Object... args) {
        super(ErrorCode.TOO_MANY_REQUESTS, detail, args);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
