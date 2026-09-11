package com.banking_system.api_server.user.ui;

import com.banking_system.api_server.user.command.application.UserDtos;
import com.banking_system.api_server.user.command.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDtos.UserResponse> signUp(@Valid @RequestBody UserDtos.SignUpRequest request) {
        UserDtos.UserResponse response = userService.signUp(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    @PostMapping("/login")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.OK)
    public UserDtos.TokenResponse login(@Valid @RequestBody UserDtos.LoginRequest request) {
        return userService.login(request);
    }
}
