package com.banking_system.api_server.user.command.domain;

import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 뱅킹 시스템 가입 사용자(member 테이블)의 애그리거트 루트.
 *
 * <p>계좌는 별도 애그리거트로 두고 {@code account.account_id} 로만 참조한다.
 * 예전 구현은 {@code @OneToMany @JoinColumn("account_number")} 로 계좌의 PK 를
 * 외래키인 것처럼 매핑해 조회 시점에 깨졌다.</p>
 */
@Entity
@Table(name = "member")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_SEQ")
    @SequenceGenerator(name = "USER_SEQ", sequenceName = "user_seq", allocationSize = 1)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Embedded
    private Password password;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 연속 로그인 실패 횟수. 로그인에 성공하면 0 으로 돌아간다. */
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    /** 이 시각까지 로그인이 차단된다. null 이면 잠기지 않은 상태. */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    protected User() {
    }

    private User(String name, String email, Password password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.failedLoginCount = 0;
    }

    public static User register(String name, String email, String rawPassword, PasswordEncoder encoder) {
        return new User(name, normalizeEmail(email), Password.encode(rawPassword, encoder));
    }

    public boolean matchPassword(String rawPassword, PasswordEncoder encoder) {
        return password.matches(rawPassword, encoder);
    }

    /**
     * 비밀번호를 교체한다. 기존과 같은 값으로는 바꿀 수 없다.
     * 재설정이 끝나면 잠금도 함께 풀어 사용자가 바로 로그인할 수 있게 한다.
     */
    public void changePassword(String rawPassword, PasswordEncoder encoder) {
        if (password.matches(rawPassword, encoder)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_CHANGED);
        }
        this.password = Password.encode(rawPassword, encoder);
        unlock();
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    /**
     * 로그인 실패를 기록하고, 임계치에 도달하면 계정을 잠근다.
     *
     * @return 이번 실패로 계정이 잠겼으면 true
     */
    public boolean recordLoginFailure(int maxAttempts, Duration lockDuration, LocalDateTime now) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= maxAttempts) {
            this.lockedUntil = now.plus(lockDuration);
            // 잠금이 풀린 뒤 한 번 더 틀렸을 때 곧바로 다시 잠기도록 카운터를 초기화한다.
            this.failedLoginCount = 0;
            return true;
        }
        return false;
    }

    public void recordLoginSuccess() {
        unlock();
    }

    private void unlock() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }
}
