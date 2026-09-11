package com.banking_system.api_server.user.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 비밀번호 재설정을 위한 일회용 토큰.
 *
 * <p>토큰 원문은 저장하지 않고 해시만 보관한다. DB 가 유출돼도 토큰을 되돌릴 수 없다.
 * 발급은 내부 API 로만 가능하며, 운영자가 사용자에게 별도 경로로 전달한다.</p>
 */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PRT_SEQ")
    @SequenceGenerator(name = "PRT_SEQ", sequenceName = "password_reset_token_seq", allocationSize = 1)
    @Column(name = "token_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 토큰 원문의 SHA-256 해시 (hex 64자) */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PasswordResetToken() {
    }

    private PasswordResetToken(Long userId, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public static PasswordResetToken issue(Long userId, String tokenHash, Duration ttl, LocalDateTime now) {
        return new PasswordResetToken(userId, tokenHash, now.plus(ttl));
    }

    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
