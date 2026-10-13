package com.trainergameserver.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.RecordComponent;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 한 필드에 검증 오류가 여러 개일 때 어떤 메시지를 보낼지 정하는 우선순위.
     * Bean Validation은 오류 순서를 보장하지 않으므로, 순서를 직접 정해야 매번 같은 메시지가 나갑니다.
     */
    private static final List<String> CONSTRAINT_PRIORITY = List.of("NotNull", "NotBlank", "NotEmpty", "Size", "Pattern");

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", e.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_USERNAME", e.getMessage()));
    }

    /** 오류가 여러 개면 요청 필드 선언 순서 → 제약 우선순위 순으로 가장 앞선 하나만 내려 줍니다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<String> fieldOrder = fieldOrderOf(e.getParameter().getParameterType());
        String message = e.getBindingResult().getFieldErrors().stream()
                .min(Comparator.<FieldError>comparingInt(err -> rank(fieldOrder, err.getField()))
                        .thenComparingInt(err -> rank(CONSTRAINT_PRIORITY, err.getCode())))
                .map(FieldError::getDefaultMessage)
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", "요청 본문(JSON) 형식이 올바르지 않습니다."));
    }

    /**
     * 위에서 처리하지 않은 모든 예외. 응답 형식을 { code, message }로 통일합니다.
     * 404/405/415 같은 Spring MVC 예외는 원래 상태 코드를 유지하고, 나머지는 500으로 내려갑니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            if (status.is4xxClientError()) {
                return ResponseEntity.status(status).body(clientError(status));
            }
        }
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    private static ErrorResponse clientError(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> new ErrorResponse("NOT_FOUND", "요청한 API를 찾을 수 없습니다.");
            case 405 -> new ErrorResponse("METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다.");
            case 415 -> new ErrorResponse("UNSUPPORTED_MEDIA_TYPE", "Content-Type은 application/json이어야 합니다.");
            default -> new ErrorResponse("INVALID_REQUEST", "요청 값이 올바르지 않습니다.");
        };
    }

    private static List<String> fieldOrderOf(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        if (components == null) {
            return List.of();
        }
        return java.util.Arrays.stream(components).map(RecordComponent::getName).toList();
    }

    private static int rank(List<String> order, String value) {
        int index = order.indexOf(value);
        return index < 0 ? Integer.MAX_VALUE : index;
    }
}
