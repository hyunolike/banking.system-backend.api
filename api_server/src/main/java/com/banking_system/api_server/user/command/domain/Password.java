package com.banking_system.api_server.user.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 값 객체. 평문은 절대 보관하지 않고 BCrypt 해시만 들고 있는다.
 */
@Embeddable
public class Password {

    @Column(name = "password", nullable = false, length = 100)
    private String encoded;

    protected Password() {
    }

    private Password(String encoded) {
        this.encoded = encoded;
    }

    public static Password encode(String rawPassword, PasswordEncoder encoder) {
        return new Password(encoder.encode(rawPassword));
    }

    /** 이미 해시된 값을 그대로 감쌀 때만 사용한다. */
    public static Password ofEncoded(String encoded) {
        return new Password(encoded);
    }

    public boolean matches(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.encoded);
    }

    public String encodedValue() {
        return encoded;
    }

    @Override
    public String toString() {
        // 로그·디버거에 해시가 새어나가지 않도록 마스킹한다.
        return "Password(****)";
    }
}
