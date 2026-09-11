package com.banking_system.api_server.user;

import com.banking_system.api_server.user.command.domain.Password;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("비밀번호는 평문으로 보관하지 않는다")
    void neverStoresRawValue() {
        Password password = Password.encode("secret-password", encoder);

        assertThat(password.encodedValue()).isNotEqualTo("secret-password");
        assertThat(password.encodedValue()).startsWith("$2");
        assertThat(password.toString()).doesNotContain("secret-password");
    }

    @Test
    @DisplayName("원문이 일치할 때만 검증에 성공한다")
    void matches() {
        Password password = Password.encode("secret-password", encoder);

        assertThat(password.matches("secret-password", encoder)).isTrue();
        assertThat(password.matches("wrong-password", encoder)).isFalse();
    }
}
