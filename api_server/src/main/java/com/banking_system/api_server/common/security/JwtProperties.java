package com.banking_system.api_server.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 서명 키와 만료 시간. 서명 키는 반드시 환경변수(JWT_SECRET)로 주입한다.
 *
 * @param secret      HS256 서명 키 (최소 32바이트)
 * @param expiration  액세스 토큰 유효 기간
 * @param issuer      토큰 발급자
 */
@ConfigurationProperties(prefix = "banking.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {

    public JwtProperties {
        if (expiration == null) {
            expiration = Duration.ofHours(1);
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "banking-system";
        }
    }
}
