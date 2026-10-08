package com.imtrainer.round.exception;

import lombok.Getter;

@Getter
public class RoundValidationException extends RuntimeException{
    private final String code;

    public RoundValidationException(String code) {
        super(code);
        this.code = code;
    }
}
