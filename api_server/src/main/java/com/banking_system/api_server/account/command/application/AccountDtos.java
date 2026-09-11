package com.banking_system.api_server.account.command.application;

import com.banking_system.api_server.account.command.domain.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record OpenAccountRequest(
            @NotBlank(message = "계좌 이름은 필수입니다.")
            @Size(max = 50, message = "계좌 이름은 50자 이하여야 합니다.")
            String name,

            @PositiveOrZero(message = "최초 입금액은 0 이상이어야 합니다.")
            @Digits(integer = 15, fraction = 4, message = "금액 형식이 올바르지 않습니다.")
            BigDecimal initialBalance) {
    }

    public record AmountRequest(
            @NotNull(message = "금액은 필수입니다.")
            @Positive(message = "금액은 0보다 커야 합니다.")
            @Digits(integer = 15, fraction = 4, message = "금액 형식이 올바르지 않습니다.")
            BigDecimal amount,

            @Size(max = 100, message = "적요는 100자 이하여야 합니다.")
            String description) {
    }

    public record TransferRequest(
            @NotNull(message = "입금 계좌번호는 필수입니다.")
            Long toAccountNumber,

            @NotNull(message = "금액은 필수입니다.")
            @Positive(message = "금액은 0보다 커야 합니다.")
            @Digits(integer = 15, fraction = 4, message = "금액 형식이 올바르지 않습니다.")
            BigDecimal amount,

            @Size(max = 100, message = "적요는 100자 이하여야 합니다.")
            String description) {
    }

    public record AccountResponse(Long accountNumber,
                                  String name,
                                  BigDecimal balance,
                                  LocalDateTime createdAt) {
    }

    public record TransactionResponse(Long id,
                                      TransactionType type,
                                      BigDecimal amount,
                                      BigDecimal balanceAfter,
                                      Long counterpartAccountNumber,
                                      String description,
                                      LocalDateTime createdAt) {
    }
}
