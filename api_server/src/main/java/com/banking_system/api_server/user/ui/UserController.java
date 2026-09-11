package com.banking_system.api_server.user.ui;

import com.banking_system.api_server.common.security.CurrentUser;
import com.banking_system.api_server.common.security.LoginUser;
import com.banking_system.api_server.user.command.application.UserDtos;
import com.banking_system.api_server.user.command.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 토큰 주인의 정보만 조회할 수 있다. 다른 사용자 조회 API 는 열지 않는다. */
    @GetMapping("/me")
    public UserDtos.UserResponse me(@CurrentUser LoginUser loginUser) {
        return userService.getMe(loginUser.id());
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUser LoginUser loginUser,
                               @Valid @RequestBody UserDtos.ChangePasswordRequest request) {
        userService.changePassword(loginUser.id(), request);
    }
}
