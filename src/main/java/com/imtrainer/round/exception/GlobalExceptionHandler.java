package com.imtrainer.round.exception;

import com.imtrainer.round.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_FORMAT");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_FIELD");
    }

    @ExceptionHandler(RoundValidationException.class)
    public ResponseEntity<ErrorResponse> handleRoundValidation(RoundValidationException e) {
        return error(HttpStatus.BAD_REQUEST, e.getCode());
    }

    @ExceptionHandler(InvalidTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTime(InvalidTimeException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_TIME");
    }

    @ExceptionHandler(RoundAlreadyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleRoundAlreadyInProgress(RoundAlreadyInProgressException e) {
        return error(HttpStatus.BAD_REQUEST, "ROUND_ALREADY_IN_PROGRESS");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), code));
    }
}
