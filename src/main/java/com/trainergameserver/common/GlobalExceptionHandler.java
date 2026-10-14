package com.trainergameserver.common;

import com.trainergameserver.player.dto.PlayerErrorResponse;
import com.trainergameserver.player.service.PlayerConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 모든 에러를 {code, message} 형식으로 바꿉니다.
 * code는 {@link ErrorCode} 이름, message는 messages.properties에서 찾은 문구입니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 한 필드에 검증 오류가 여러 개일 때 어떤 메시지를 보낼지 정하는 우선순위.
     * Bean Validation은 오류 순서를 보장하지 않으므로, 순서를 직접 정해야 매번 같은 메시지가 나갑니다.
     */
    private static final List<String> CONSTRAINT_PRIORITY = List.of("NotNull", "NotBlank", "NotEmpty", "Size", "Min", "Max", "Pattern");

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /** 서비스에서 던진 예외. 429는 Retry-After 헤더, 플레이데이터 검증 실패는 playerData가 추가됩니다. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handleApi(ApiException e) {
        ErrorCode code = e.getCode();
        String message = message(e.getMessageKey(), e.getArgs());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(code.getStatus());

        if (e instanceof TooManyRequestsException tooMany) {
            response.header("Retry-After", String.valueOf(tooMany.getRetryAfterSeconds()));
        }
        if (e instanceof PlayerConflictException conflict) {
            return response.body(new PlayerErrorResponse(code.name(), message, conflict.getPlayerData()));
        }
        return response.body(new ErrorResponse(code.name(), message));
    }

    /**
     * 요청 DTO 검증 실패. 메시지는 어노테이션의 {validation.*} 키로 messages.properties에서 찾아집니다.
     * 오류가 여러 개면 요청 필드 선언 순서 → 제약 우선순위 순으로 가장 앞선 하나만 내려 줍니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<String> fieldOrder = fieldOrderOf(e.getParameter().getParameterType());
        String message = e.getBindingResult().getFieldErrors().stream()
                .min(Comparator.<FieldError>comparingInt(err -> rank(fieldOrder, err.getField()))
                        .thenComparingInt(err -> rank(CONSTRAINT_PRIORITY, err.getCode())))
                .map(FieldError::getDefaultMessage)
                .orElseGet(() -> message("error.INVALID_REQUEST"));
        return error(ErrorCode.INVALID_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return error(ErrorCode.INVALID_REQUEST, message("error.INVALID_REQUEST.json"));
    }

    /** 쿼리 파라미터 타입 오류 (예: /leaderboard?size=abc). 처리하지 않으면 500으로 내려갑니다. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(ErrorCode.INVALID_REQUEST, message("error.INVALID_REQUEST.type"));
    }

    /**
     * 위에서 처리하지 않은 모든 예외.
     * 404/405/415 같은 Spring MVC 예외는 원래 상태 코드를 유지하고, 나머지는 500으로 내려갑니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            if (status.is4xxClientError()) {
                ErrorCode code = ErrorCode.ofClientStatus(status.value());
                return ResponseEntity.status(status).body(new ErrorResponse(code.name(), message("error." + code.name())));
            }
        }
        log.error("처리되지 않은 예외", e);
        return error(ErrorCode.INTERNAL_ERROR, message("error.INTERNAL_ERROR"));
    }

    private ResponseEntity<ErrorResponse> error(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus()).body(new ErrorResponse(code.name(), message));
    }

    /** 메시지 키가 없으면 서버 로그를 남기고 기본 문구를 씁니다(응답이 500으로 바뀌지 않도록). */
    private String message(String key, Object... args) {
        try {
            return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            log.error("messages.properties에 메시지 키가 없습니다: {}", key);
            return messageSource.getMessage("error.INTERNAL_ERROR", null, "요청을 처리할 수 없습니다.", LocaleContextHolder.getLocale());
        }
    }

    private static List<String> fieldOrderOf(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        if (components == null) {
            return List.of();
        }
        return Arrays.stream(components).map(RecordComponent::getName).toList();
    }

    private static int rank(List<String> order, String value) {
        int index = order.indexOf(value);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
