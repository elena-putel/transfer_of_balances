package com.beltelecom.transfer.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ValidationFailedException extends RuntimeException {

    private final List<String> errors;

    public ValidationFailedException(List<String> errors) {
        super("Validation failed: " + String.join("; ", errors));
        this.errors = errors;
    }
}
