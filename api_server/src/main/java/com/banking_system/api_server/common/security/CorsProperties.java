package com.banking_system.api_server.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 브라우저에서 직접 호출하는 프론트엔드를 위한 CORS 허용 목록.
 *
 * <p>기본값은 빈 목록이다. 허용 오리진을 명시하지 않으면 교차 출처 요청은 모두 막힌다.
 * 토큰을 Authorization 헤더로 보내므로 쿠키 자격증명은 허용하지 않는다.</p>
 *
 * @param allowedOrigins 허용할 오리진 (예: https://app.example.com)
 * @param allowedMethods 허용할 HTTP 메서드
 * @param maxAge         프리플라이트 캐시 시간
 */
@ConfigurationProperties(prefix = "banking.cors")
public record CorsProperties(List<String> allowedOrigins,
                             List<String> allowedMethods,
                             Duration maxAge) {

    public CorsProperties {
        if (allowedOrigins == null) {
            allowedOrigins = List.of();
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            allowedMethods = List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS");
        }
        if (maxAge == null) {
            maxAge = Duration.ofHours(1);
        }
    }

    public boolean enabled() {
        return !allowedOrigins.isEmpty();
    }
}
