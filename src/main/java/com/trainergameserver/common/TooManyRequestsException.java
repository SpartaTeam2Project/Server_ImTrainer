package com.trainergameserver.common;

/** 요청 횟수 제한에 걸린 경우. retryAfterSeconds 뒤에 다시 시도할 수 있습니다. */
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
