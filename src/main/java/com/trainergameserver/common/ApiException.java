package com.trainergameserver.common;

import java.util.Arrays;

/**
 * 클라에 {code, message}로 내려가는 예외. GlobalExceptionHandler가 HTTP 응답으로 바꿉니다.
 * <p>
 * 메시지는 코드에 쓰지 않고 messages.properties의 키로 지정합니다.
 * <ul>
 *   <li>{@code new ApiException(ErrorCode.MAX_LEVEL)} → error.MAX_LEVEL</li>
 *   <li>{@code new ApiException(ErrorCode.MISMATCH, "cost", 650)} → error.MISMATCH.cost, {0}에 650</li>
 * </ul>
 * 숫자 인자는 문자열로 바꿔 넣어 1,500처럼 쉼표가 붙지 않게 합니다.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final String messageKey;
    private final Object[] args;

    public ApiException(ErrorCode code) {
        this(code, null);
    }

    public ApiException(ErrorCode code, String detail, Object... args) {
        super(code + (detail == null ? "" : "." + detail) + (args.length == 0 ? "" : " " + Arrays.toString(args)));
        this.code = code;
        this.messageKey = "error." + code.name() + (detail == null ? "" : "." + detail);
        this.args = Arrays.stream(args).map(String::valueOf).toArray();
    }

    public ErrorCode getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
