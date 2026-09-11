package com.banking_system.api_server.common.error;

/**
 * 도메인 규칙 위반을 표현하는 예외. {@link GlobalExceptionHandler} 가 HTTP 응답으로 변환한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
