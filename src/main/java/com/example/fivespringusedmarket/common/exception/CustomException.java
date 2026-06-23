package com.example.fivespringusedmarket.common.exception;

/**
 * 서비스 계층에서 발생하는 비즈니스 예외다.
 */
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
