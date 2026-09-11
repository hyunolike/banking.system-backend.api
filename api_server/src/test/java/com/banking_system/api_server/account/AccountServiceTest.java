package com.banking_system.api_server.account;

import com.banking_system.api_server.account.command.application.AccountDtos;
import com.banking_system.api_server.account.command.application.AccountService;
import com.banking_system.api_server.account.command.domain.TransactionType;
import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import com.banking_system.api_server.user.command.application.UserDtos;
import com.banking_system.api_server.user.command.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    private Long ownerId;
    private Long otherId;

    @BeforeEach
    void setUp() {
        ownerId = newUser().id();
        otherId = newUser().id();
    }

    private UserDtos.UserResponse newUser() {
        String email = UUID.randomUUID() + "@banking.test";
        return userService.signUp(new UserDtos.SignUpRequest("테스터", email, "password1234"));
    }

    private AccountDtos.AccountResponse openAccount(Long userId, String balance) {
        return accountService.open(userId, new AccountDtos.OpenAccountRequest("테스트계좌", new BigDecimal(balance)));
    }

    @Test
    @DisplayName("계좌를 개설하면 최초 입금이 원장에 남는다")
    void openWritesLedger() {
        AccountDtos.AccountResponse account = openAccount(ownerId, "10000");

        List<AccountDtos.TransactionResponse> transactions =
                accountService.findTransactions(ownerId, account.accountNumber(), PageRequest.of(0, 10));

        assertThat(account.balance()).isEqualByComparingTo("10000");
        assertThat(transactions).singleElement()
                .satisfies(tx -> {
                    assertThat(tx.type()).isEqualTo(TransactionType.DEPOSIT);
                    assertThat(tx.balanceAfter()).isEqualByComparingTo("10000");
                });
    }

    @Test
    @DisplayName("이체하면 양쪽 잔액과 원장 2건이 함께 기록된다")
    void transfer() {
        AccountDtos.AccountResponse from = openAccount(ownerId, "10000");
        AccountDtos.AccountResponse to = openAccount(otherId, "0");

        accountService.transfer(ownerId, from.accountNumber(),
                new AccountDtos.TransferRequest(to.accountNumber(), new BigDecimal("2500.50"), "월세"));

        assertThat(accountService.findMyAccount(ownerId, from.accountNumber()).balance())
                .isEqualByComparingTo("7499.50");
        assertThat(accountService.findMyAccount(otherId, to.accountNumber()).balance())
                .isEqualByComparingTo("2500.50");

        assertThat(accountService.findTransactions(ownerId, from.accountNumber(), PageRequest.of(0, 10)))
                .first()
                .satisfies(tx -> {
                    assertThat(tx.type()).isEqualTo(TransactionType.TRANSFER_OUT);
                    assertThat(tx.counterpartAccountNumber()).isEqualTo(to.accountNumber());
                });
        assertThat(accountService.findTransactions(otherId, to.accountNumber(), PageRequest.of(0, 10)))
                .first()
                .satisfies(tx -> assertThat(tx.type()).isEqualTo(TransactionType.TRANSFER_IN));
    }

    @Test
    @DisplayName("잔액이 부족하면 이체가 전부 롤백된다")
    void transferRollsBackWhenInsufficient() {
        AccountDtos.AccountResponse from = openAccount(ownerId, "1000");
        AccountDtos.AccountResponse to = openAccount(otherId, "0");

        assertThatThrownBy(() -> accountService.transfer(ownerId, from.accountNumber(),
                new AccountDtos.TransferRequest(to.accountNumber(), new BigDecimal("5000"), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        assertThat(accountService.findMyAccount(ownerId, from.accountNumber()).balance())
                .isEqualByComparingTo("1000");
        assertThat(accountService.findMyAccount(otherId, to.accountNumber()).balance())
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("남의 계좌에서는 출금할 수 없다")
    void cannotWithdrawFromOthersAccount() {
        AccountDtos.AccountResponse victim = openAccount(otherId, "10000");

        assertThatThrownBy(() -> accountService.withdraw(ownerId, victim.accountNumber(),
                new AccountDtos.AmountRequest(new BigDecimal("100"), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ACCOUNT_OWNER);
    }

    @Test
    @DisplayName("같은 계좌로는 이체할 수 없다")
    void cannotTransferToSelf() {
        AccountDtos.AccountResponse account = openAccount(ownerId, "10000");

        assertThatThrownBy(() -> accountService.transfer(ownerId, account.accountNumber(),
                new AccountDtos.TransferRequest(account.accountNumber(), new BigDecimal("100"), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);
    }

    @Test
    @DisplayName("같은 이메일로는 두 번 가입할 수 없다")
    void duplicateEmail() {
        String email = UUID.randomUUID() + "@banking.test";
        userService.signUp(new UserDtos.SignUpRequest("테스터", email, "password1234"));

        assertThatThrownBy(() -> userService.signUp(new UserDtos.SignUpRequest("테스터2", email, "password1234")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
