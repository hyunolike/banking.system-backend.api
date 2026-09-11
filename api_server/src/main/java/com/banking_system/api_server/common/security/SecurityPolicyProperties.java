package com.banking_system.api_server.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 로그인 잠금 / 비밀번호 재설정 정책.
 *
 * @param login         로그인 실패 처리 정책
 * @param passwordReset 재설정 토큰 정책
 */
@ConfigurationProperties(prefix = "banking.security")
public record SecurityPolicyProperties(Login login, PasswordReset passwordReset) {

    public SecurityPolicyProperties {
        if (login == null) {
            login = new Login(0, null);
        }
        if (passwordReset == null) {
            passwordReset = new PasswordReset(null);
        }
    }

    /**
     * @param maxAttempts  연속 실패 허용 횟수 (기본 5)
     * @param lockDuration 임계치 도달 시 잠기는 시간 (기본 15분)
     */
    public record Login(int maxAttempts, Duration lockDuration) {

        public Login {
            if (maxAttempts <= 0) {
                maxAttempts = 5;
            }
            if (lockDuration == null) {
                lockDuration = Duration.ofMinutes(15);
            }
        }
    }

    /**
     * @param ttl 재설정 토큰 유효 기간 (기본 30분)
     */
    public record PasswordReset(Duration ttl) {

        public PasswordReset {
            if (ttl == null) {
                ttl = Duration.ofMinutes(30);
            }
        }
    }
}
