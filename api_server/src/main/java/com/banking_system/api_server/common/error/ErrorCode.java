package com.banking_system.api_server.common.error;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드.
 * 클라이언트는 HTTP 상태값이 아니라 {@code code} 를 보고 분기한다.
 */
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 내부 오류가 발생했습니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "U004", "로그인 시도가 너무 많아 계정이 잠겼습니다."),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "U005", "재설정 토큰이 유효하지 않거나 만료되었습니다."),
    PASSWORD_NOT_CHANGED(HttpStatus.BAD_REQUEST, "U006", "새 비밀번호가 기존 비밀번호와 같습니다."),

    // 계좌
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "존재하지 않는 계좌입니다."),
    NOT_ACCOUNT_OWNER(HttpStatus.FORBIDDEN, "A002", "본인 소유의 계좌가 아닙니다."),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "A003", "잔액이 부족합니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "A004", "거래 금액은 0보다 커야 합니다."),
    SAME_ACCOUNT_TRANSFER(HttpStatus.BAD_REQUEST, "A005", "출금 계좌와 입금 계좌가 동일합니다."),

    // 친구
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "존재하지 않는 친구입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
