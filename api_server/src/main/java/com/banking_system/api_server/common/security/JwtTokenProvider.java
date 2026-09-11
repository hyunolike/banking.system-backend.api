package com.banking_system.api_server.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * 액세스 토큰 발급/검증. 상태를 두지 않으므로 서버 확장 시 세션 공유가 필요 없다.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;
    private final String issuer;

    public JwtTokenProvider(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "banking.jwt.secret 이 비어 있거나 너무 짧습니다. 최소 " + MIN_SECRET_BYTES
                            + "바이트 값을 JWT_SECRET 환경변수로 주입하세요.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.expiration();
        this.issuer = properties.issuer();
    }

    public String createToken(Long userId, String email) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Duration getExpiration() {
        return expiration;
    }

    /**
     * 토큰이 유효하면 인증 주체를, 유효하지 않으면 {@link Optional#empty()} 를 돌려준다.
     * 서명 위조/만료 여부를 호출 측이 구분할 필요가 없으므로 예외를 밖으로 던지지 않는다.
     */
    public Optional<LoginUser> parse(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(new LoginUser(Long.valueOf(claims.getSubject()), claims.get("email", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("invalid jwt :: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
