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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 로그인 잠금. 테스트 프로파일의 임계치는 3회다.
 */
@SpringBootTest
@ActiveProfiles("test")
class LoginLockTest {

    private static final String PASSWORD = "password1234";

    @Autowired
    private UserService userService;

    private String signUpAndGetEmail() {
        String email = UUID.randomUUID() + "@banking.test";
        userService.signUp(new UserDtos.SignUpRequest("테스터", email, PASSWORD));
        return email;
    }

    private ErrorCode loginExpectingFailure(String email, String password) {
        try {
            userService.login(new UserDtos.LoginRequest(email, password));
        } catch (BusinessException e) {
            return e.getErrorCode();
        }
        throw new AssertionError("로그인이 실패해야 하는데 성공했습니다.");
    }

    @Test
    @DisplayName("임계치만큼 실패하면 계정이 잠기고, 올바른 비밀번호로도 로그인되지 않는다")
    void locksAfterRepeatedFailures() {
        String email = signUpAndGetEmail();

        assertThat(loginExpectingFailure(email, "wrong-1")).isEqualTo(ErrorCode.LOGIN_FAILED);
        assertThat(loginExpectingFailure(email, "wrong-2")).isEqualTo(ErrorCode.LOGIN_FAILED);
        // 3회째에 잠기지만, 이 호출 자체는 비밀번호가 틀린 것이므로 LOGIN_FAILED 다.
        assertThat(loginExpectingFailure(email, "wrong-3")).isEqualTo(ErrorCode.LOGIN_FAILED);

        // 잠긴 뒤에는 비밀번호가 맞아도 거부된다.
        assertThat(loginExpectingFailure(email, PASSWORD)).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    @DisplayName("실패 도중 로그인에 성공하면 실패 횟수가 초기화된다")
    void successResetsFailureCount() {
        String email = signUpAndGetEmail();

        loginExpectingFailure(email, "wrong-1");
        loginExpectingFailure(email, "wrong-2");
        assertThat(userService.login(new UserDtos.LoginRequest(email, PASSWORD)).accessToken()).isNotBlank();

        // 카운터가 0 으로 돌아갔으므로 두 번 더 틀려도 아직 잠기지 않는다.
        loginExpectingFailure(email, "wrong-3");
        loginExpectingFailure(email, "wrong-4");
        assertThat(userService.login(new UserDtos.LoginRequest(email, PASSWORD)).accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("비밀번호를 재설정하면 잠금도 함께 풀린다")
    void resetPasswordUnlocksAccount() {
        String email = signUpAndGetEmail();
        loginExpectingFailure(email, "wrong-1");
        loginExpectingFailure(email, "wrong-2");
        loginExpectingFailure(email, "wrong-3");
        assertThat(loginExpectingFailure(email, PASSWORD)).isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        String token = userService.issueResetToken(new UserDtos.IssueResetTokenRequest(email)).token();
        userService.resetPassword(new UserDtos.ResetPasswordRequest(token, "recovered-password-1"));

        assertThat(userService.login(new UserDtos.LoginRequest(email, "recovered-password-1")).accessToken())
                .isNotBlank();
    }

    @Test
    @DisplayName("존재하지 않는 이메일은 잠금과 무관하게 항상 같은 에러를 돌려준다")
    void unknownEmailIsIndistinguishable() {
        for (int i = 0; i < 5; i++) {
            assertThat(loginExpectingFailure("nobody-" + UUID.randomUUID() + "@banking.test", PASSWORD))
                    .isEqualTo(ErrorCode.LOGIN_FAILED);
        }
    }
}
