package com.banking_system.api_server.user.ui;

import com.banking_system.api_server.user.command.application.UserDtos;
import com.banking_system.api_server.user.command.application.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 전용 사용자 관리. {@code X-Internal-Api-Key} 헤더로만 접근할 수 있다.
 */
@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 비밀번호 재설정 토큰을 발급한다.
     * 응답의 토큰 원문은 여기서만 확인할 수 있으므로, 본인 확인 후 사용자에게 전달한다.
     */
    @PostMapping("/password-reset-tokens")
    public UserDtos.IssueResetTokenResponse issueResetToken(
            @Valid @RequestBody UserDtos.IssueResetTokenRequest request) {
        return userService.issueResetToken(request);
    }
}
