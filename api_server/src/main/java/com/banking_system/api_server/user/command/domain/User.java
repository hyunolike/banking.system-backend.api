package com.banking_system.api_server.user.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    protected User() {
    }

    private User(String name, String email, Password password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    public static User register(String name, String email, String rawPassword, PasswordEncoder encoder) {
        return new User(name, normalizeEmail(email), Password.encode(rawPassword, encoder));
    }

    public boolean matchPassword(String rawPassword, PasswordEncoder encoder) {
        return password.matches(rawPassword, encoder);
    }

    public void changePassword(String rawPassword, PasswordEncoder encoder) {
        this.password = Password.encode(rawPassword, encoder);
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
}
