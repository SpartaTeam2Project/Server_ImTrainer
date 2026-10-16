package com.trainergameserver.round.validation;

import com.trainergameserver.common.ApiException;
import com.trainergameserver.common.ErrorCode;

/**
 * 라운드 결과 검증 실패.
 * 클라이언트 응답은 ApiException 과 같고(code + 공통 문구), reason 은 서버 로그에만 남깁니다.
 * 어떤 기준에 걸렸는지 응답으로 알려주면 조작 힌트가 되기 때문
 */
public class RoundValidationException extends ApiException {

    private final String reason;

    public RoundValidationException(ErrorCode code, String reason) {
        super(code);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
