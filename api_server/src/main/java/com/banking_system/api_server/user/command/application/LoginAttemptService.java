package com.banking_system.api_server.user.command.application;

import com.banking_system.api_server.common.security.SecurityPolicyProperties;
import com.banking_system.api_server.user.command.domain.User;
import com.banking_system.api_server.user.command.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 로그인 시도 결과를 기록한다.
 *
 * <p>실패 기록은 곧바로 {@code LOGIN_FAILED} 예외로 이어지는데, 같은 트랜잭션에 두면
 * 롤백되면서 실패 횟수가 사라진다. 그래서 별도 트랜잭션({@code REQUIRES_NEW})에서
 * 커밋하고, 예외는 호출한 쪽에서 던진다.</p>
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserRepository userRepository;
    private final SecurityPolicyProperties policy;

    public LoginAttemptService(UserRepository userRepository, SecurityPolicyProperties policy) {
        this.userRepository = userRepository;
        this.policy = policy;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            boolean locked = user.recordLoginFailure(
                    policy.login().maxAttempts(), policy.login().lockDuration(), LocalDateTime.now());
            if (locked) {
                log.warn("account locked by repeated login failure :: userId={} until={}",
                        userId, user.getLockedUntil());
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long userId) {
        userRepository.findById(userId).ifPresent(User::recordLoginSuccess);
    }
}
