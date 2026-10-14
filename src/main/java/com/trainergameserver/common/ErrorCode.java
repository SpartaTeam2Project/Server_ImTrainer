package com.trainergameserver.common;

import org.springframework.http.HttpStatus;

/**
 * API 에러 코드와 HTTP 상태. 응답 본문의 code에는 이 이름이 그대로 내려갑니다.
 * 사용자에게 보여 줄 메시지는 resources/messages.properties의 error.{코드}[.{상세}] 키에 있습니다.
 */
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    // 인증
    DUPLICATE_USERNAME(HttpStatus.CONFLICT),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),

    // 플레이데이터 검증 실패 (응답에 서버 기준 playerData 포함)
    /** 클라 계산값(cost, refund, balanceAfter, toLevel, currencyId)이 서버 계산과 다름 */
    MISMATCH(HttpStatus.CONFLICT),
    NOT_ENOUGH_CURRENCY(HttpStatus.CONFLICT),
    /** 트레이닝 해금 조건 미충족, 인트로에서 받을 수 없는 트레이너 */
    REQUIREMENT_NOT_MET(HttpStatus.CONFLICT),
    MAX_LEVEL(HttpStatus.CONFLICT),
    NOT_OWNED(HttpStatus.CONFLICT),
    INTRO_ALREADY_DONE(HttpStatus.CONFLICT),
    /** 마스터데이터에 없는 트레이닝·포켓몬 id */
    UNKNOWN_ID(HttpStatus.CONFLICT),

    // 라운드 (종료 결과 검증 실패는 400, 메시지는 모두 같은 문구)
    ROUND_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_TIME(HttpStatus.BAD_REQUEST),
    INVALID_BOSS(HttpStatus.BAD_REQUEST),
    INVALID_KILL(HttpStatus.BAD_REQUEST),
    INVALID_EXP(HttpStatus.BAD_REQUEST),
    INVALID_LEVEL(HttpStatus.BAD_REQUEST),
    INVALID_MONSTERBALL(HttpStatus.BAD_REQUEST),
    INVALID_ENTRY(HttpStatus.BAD_REQUEST);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 404/405/415 등 Spring MVC가 정한 상태 코드에 맞는 에러 코드 (없으면 INVALID_REQUEST) */
    public static ErrorCode ofClientStatus(int status) {
        return switch (status) {
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> INVALID_REQUEST;
        };
    }
}
