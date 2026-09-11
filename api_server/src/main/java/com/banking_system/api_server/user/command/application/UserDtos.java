package com.banking_system.api_server.user.command.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원 커맨드 요청/응답 DTO 모음.
 */
public final class UserDtos {

    private UserDtos() {
    }

    public record SignUpRequest(
            @NotBlank(message = "이름은 필수입니다.")
            @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
            String name,

            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 아닙니다.")
            @Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다.")
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
            String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 아닙니다.")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "현재 비밀번호는 필수입니다.")
            String currentPassword,

            @NotBlank(message = "새 비밀번호는 필수입니다.")
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
            String newPassword) {
    }

    public record IssueResetTokenRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 아닙니다.")
            String email) {
    }

    /**
     * 재설정 토큰 발급 결과. 토큰 원문은 이 응답에서만 볼 수 있고 DB 에는 해시만 남는다.
     * 운영자가 본인 확인 후 사용자에게 별도 경로로 전달한다.
     */
    public record IssueResetTokenResponse(Long userId, String token, java.time.LocalDateTime expiresAt) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "재설정 토큰은 필수입니다.")
            String token,

            @NotBlank(message = "새 비밀번호는 필수입니다.")
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
            String newPassword) {
    }

    /** 비밀번호 해시는 어떤 응답에도 포함하지 않는다. */
    public record UserResponse(Long id, String name, String email) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
    }
}
