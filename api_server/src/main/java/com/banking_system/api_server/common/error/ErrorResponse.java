package com.banking_system.api_server.common.error;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 에러 응답의 공통 바디.
 *
 * @param code    {@link ErrorCode} 의 코드값
 * @param message 사용자에게 보여줄 메시지
 * @param errors  필드 단위 검증 실패 내역 (없으면 빈 리스트)
 */
public record ErrorResponse(String code,
                            String message,
                            List<FieldError> errors,
                            LocalDateTime timestamp) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), message, errors, LocalDateTime.now());
    }

    public record FieldError(String field, String reason) {
    }
}
