package com.banking_system.api_server.user.command.application;

import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import com.banking_system.api_server.common.security.JwtTokenProvider;
import com.banking_system.api_server.common.security.SecurityPolicyProperties;
import com.banking_system.api_server.user.command.domain.PasswordResetToken;
import com.banking_system.api_server.user.command.domain.PasswordResetTokenRepository;
import com.banking_system.api_server.user.command.domain.User;
import com.banking_system.api_server.user.command.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SecurityPolicyProperties policy;

    public UserService(UserRepository userRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordResetTokenGenerator tokenGenerator,
                       LoginAttemptService loginAttemptService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       SecurityPolicyProperties policy) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.policy = policy;
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

    /**
     * 로그인.
     *
     * <p>연속 실패가 임계치를 넘으면 계정을 일정 시간 잠근다. 잠긴 계정에는
     * {@code ACCOUNT_LOCKED} 를 돌려주는데, 이는 해당 이메일의 존재를 알려주는 대신
     * 정상 사용자가 "왜 로그인이 안 되는지" 알 수 있게 하는 쪽을 택한 것이다.</p>
     */
    public UserDtos.TokenResponse login(UserDtos.LoginRequest request) {
        String email = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                // 존재하지 않는 이메일인지 비밀번호가 틀린 것인지 구분해주지 않는다.
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (user.isLocked(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "로그인 시도가 너무 많아 계정이 잠겼습니다. " + user.getLockedUntil() + " 이후 다시 시도하세요.");
        }

        if (!user.matchPassword(request.password(), passwordEncoder)) {
            // 별도 트랜잭션에서 커밋해야 아래 예외로 롤백되지 않는다.
            loginAttemptService.recordFailure(user.getId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        loginAttemptService.recordSuccess(user.getId());

        String token = tokenProvider.createToken(user.getId(), user.getEmail());
        return new UserDtos.TokenResponse(token, "Bearer",
                tokenProvider.getExpiration().toSeconds(), toResponse(user));
    }

    public UserDtos.UserResponse getMe(Long userId) {
        return userRepository.findById(userId)
                .map(UserService::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** 로그인한 사용자가 현재 비밀번호를 확인하고 직접 바꾼다. */
    @Transactional
    public void changePassword(Long userId, UserDtos.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.matchPassword(request.currentPassword(), passwordEncoder)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "현재 비밀번호가 올바르지 않습니다.");
        }

        user.changePassword(request.newPassword(), passwordEncoder);
        log.info("password changed :: userId={}", userId);
    }

    /**
     * 재설정 토큰 발급. 내부 API 로만 호출할 수 있다.
     *
     * <p>V2 마이그레이션으로 기존 평문 비밀번호가 무효화된 사용자는 로그인 자체가 막혀
     * {@link #changePassword} 를 쓸 수 없다. 그 복구 경로가 이 토큰이다.
     * 메일 발송 수단이 없으므로 운영자가 본인 확인 후 직접 전달한다.</p>
     */
    @Transactional
    public UserDtos.IssueResetTokenResponse issueResetToken(UserDtos.IssueResetTokenRequest request) {
        String email = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        // 한 사용자에게 유효한 토큰이 여러 개 떠다니지 않도록 이전 것을 먼저 닫는다.
        resetTokenRepository.invalidateAllByUserId(user.getId(), now);

        String rawToken = tokenGenerator.generate();
        PasswordResetToken token = resetTokenRepository.save(PasswordResetToken.issue(
                user.getId(), tokenGenerator.hash(rawToken), policy.passwordReset().ttl(), now));

        log.info("password reset token issued :: userId={} expiresAt={}", user.getId(), token.getExpiresAt());
        return new UserDtos.IssueResetTokenResponse(user.getId(), rawToken, token.getExpiresAt());
    }

    /** 발급받은 토큰으로 비밀번호를 재설정한다. 토큰은 1회만 쓸 수 있다. */
    @Transactional
    public void resetPassword(UserDtos.ResetPasswordRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = resetTokenRepository.findByTokenHash(tokenGenerator.hash(request.token()))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.changePassword(request.newPassword(), passwordEncoder);
        token.markUsed(now);

        log.info("password reset :: userId={}", user.getId());
    }

    private static UserDtos.UserResponse toResponse(User user) {
        return new UserDtos.UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
