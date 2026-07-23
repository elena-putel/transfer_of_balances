package com.beltelecom.transfer.exception;

import lombok.Getter;

@Getter
public class TransferProcessingException extends RuntimeException {

    private final String errorCode;

    public TransferProcessingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TransferProcessingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
