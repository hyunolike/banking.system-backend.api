package com.banking_system.api_server.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 스케줄러 등 내부 서비스가 {@code /api/internal/**} 를 호출할 때 사용하는 공유 키.
 *
 * @param apiKey 내부 호출자 인증 키 (INTERNAL_API_KEY 환경변수)
 */
@ConfigurationProperties(prefix = "banking.internal")
public record InternalApiProperties(String apiKey) {
}
