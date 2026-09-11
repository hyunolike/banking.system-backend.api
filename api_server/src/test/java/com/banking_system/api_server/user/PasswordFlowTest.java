package com.banking_system.api_server.user;

import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import com.banking_system.api_server.user.command.application.UserDtos;
import com.banking_system.api_server.user.command.application.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 비밀번호 변경/재설정. 특히 V2 마이그레이션으로 로그인이 막힌 사용자가
 * 재설정 토큰으로 복구할 수 있는지를 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PasswordFlowTest {

    private static final String OLD_PASSWORD = "password1234";
    private static final String NEW_PASSWORD = "new-password-5678";

    @Autowired
    private UserService userService;

    private String newEmail() {
        return UUID.randomUUID() + "@banking.test";
    }

    private UserDtos.UserResponse signUp(String email) {
        return userService.signUp(new UserDtos.SignUpRequest("테스터", email, OLD_PASSWORD));
    }

    @Test
    @DisplayName("현재 비밀번호를 확인하고 바꾸면 새 비밀번호로 로그인된다")
    void changePassword() {
        String email = newEmail();
        Long userId = signUp(email).id();

        userService.changePassword(userId, new UserDtos.ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD));

        assertThatThrownBy(() -> userService.login(new UserDtos.LoginRequest(email, OLD_PASSWORD)))
                .isInstanceOf(BusinessException.class);
        assertThat(userService.login(new UserDtos.LoginRequest(email, NEW_PASSWORD)).accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 바꿀 수 없다")
    void changePasswordWithWrongCurrent() {
        Long userId = signUp(newEmail()).id();

        assertThatThrownBy(() -> userService.changePassword(userId,
                new UserDtos.ChangePasswordRequest("wrong-password", NEW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("같은 비밀번호로는 바꿀 수 없다")
    void cannotReuseSamePassword() {
        Long userId = signUp(newEmail()).id();

        assertThatThrownBy(() -> userService.changePassword(userId,
                new UserDtos.ChangePasswordRequest(OLD_PASSWORD, OLD_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_NOT_CHANGED);
    }

    @Test
    @DisplayName("재설정 토큰으로 비밀번호를 되살릴 수 있다")
    void resetPassword() {
        String email = newEmail();
        signUp(email);

        String token = userService.issueResetToken(new UserDtos.IssueResetTokenRequest(email)).token();
        userService.resetPassword(new UserDtos.ResetPasswordRequest(token, NEW_PASSWORD));

        assertThat(userService.login(new UserDtos.LoginRequest(email, NEW_PASSWORD)).accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("재설정 토큰은 한 번만 쓸 수 있다")
    void resetTokenIsSingleUse() {
        String email = newEmail();
        signUp(email);
        String token = userService.issueResetToken(new UserDtos.IssueResetTokenRequest(email)).token();

        userService.resetPassword(new UserDtos.ResetPasswordRequest(token, NEW_PASSWORD));

        assertThatThrownBy(() -> userService.resetPassword(
                new UserDtos.ResetPasswordRequest(token, "another-password-99")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
    }

    @Test
    @DisplayName("토큰을 새로 발급하면 이전 토큰은 무효가 된다")
    void issuingNewTokenInvalidatesPrevious() {
        String email = newEmail();
        signUp(email);

        String first = userService.issueResetToken(new UserDtos.IssueResetTokenRequest(email)).token();
        String second = userService.issueResetToken(new UserDtos.IssueResetTokenRequest(email)).token();

        assertThatThrownBy(() -> userService.resetPassword(new UserDtos.ResetPasswordRequest(first, NEW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);

        assertThatCode(() -> userService.resetPassword(new UserDtos.ResetPasswordRequest(second, NEW_PASSWORD)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("없는 토큰으로는 재설정할 수 없다")
    void unknownToken() {
        assertThatThrownBy(() -> userService.resetPassword(
                new UserDtos.ResetPasswordRequest("not-a-real-token", NEW_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
    }
}
