package com.banking_system.api_server.account;

import com.banking_system.api_server.account.command.domain.Account;
import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 잔액 규칙은 DB 없이 도메인 단위로 검증한다. */
class AccountTest {

    private Account account(String balance) {
        return Account.open(1L, "주계좌", new BigDecimal(balance));
    }

    @Test
    @DisplayName("입금하면 잔액이 늘어난다")
    void deposit() {
        Account account = account("1000");

        account.deposit(new BigDecimal("500.25"));

        assertThat(account.getBalance()).isEqualByComparingTo("1500.25");
    }

    @Test
    @DisplayName("출금하면 잔액이 줄어든다")
    void withdraw() {
        Account account = account("1000");

        account.withdraw(new BigDecimal("300"));

        assertThat(account.getBalance()).isEqualByComparingTo("700");
    }

    @Test
    @DisplayName("잔액보다 많이 출금할 수 없다")
    void withdrawOverBalance() {
        Account account = account("1000");

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("1000.0001")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("0 이하 금액은 거래할 수 없다")
    void nonPositiveAmount() {
        Account account = account("1000");

        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("-1"))).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("허용 스케일을 넘는 금액은 반올림하지 않고 거절한다")
    void tooPreciseAmount() {
        Account account = account("1000");

        assertThatThrownBy(() -> account.deposit(new BigDecimal("0.00001")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("소유자가 아니면 접근할 수 없다")
    void requireOwner() {
        Account account = account("1000");

        assertThat(account.isOwnedBy(1L)).isTrue();
        assertThatThrownBy(() -> account.requireOwner(2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ACCOUNT_OWNER);
    }
}
