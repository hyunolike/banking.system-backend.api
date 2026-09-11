package com.banking_system.api_server.user.command.application;

import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import com.banking_system.api_server.common.security.JwtTokenProvider;
import com.banking_system.api_server.user.command.domain.User;
import com.banking_system.api_server.user.command.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public UserDtos.UserResponse signUp(UserDtos.SignUpRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        try {
            User saved = userRepository.saveAndFlush(
                    User.register(request.name(), email, request.password(), passwordEncoder));
            log.info("user registered :: id={}", saved.getId());
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // 중복 검사와 INSERT 사이에 다른 요청이 끼어든 경우 unique 제약이 최종 방어선이 된다.
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public UserDtos.TokenResponse login(UserDtos.LoginRequest request) {
        String email = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                // 존재하지 않는 이메일인지 비밀번호가 틀린 것인지 구분해주지 않는다.
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!user.matchPassword(request.password(), passwordEncoder)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String token = tokenProvider.createToken(user.getId(), user.getEmail());
        return new UserDtos.TokenResponse(token, "Bearer",
                tokenProvider.getExpiration().toSeconds(), toResponse(user));
    }

    public UserDtos.UserResponse getMe(Long userId) {
        return userRepository.findById(userId)
                .map(UserService::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private static UserDtos.UserResponse toResponse(User user) {
        return new UserDtos.UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
