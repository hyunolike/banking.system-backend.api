package com.banking_system.api_server.account.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래 원장 한 줄. 잔액만 들고 있으면 "왜 이 잔액인지" 추적할 수 없어 추가했다.
 * 이체 1건은 출금 계좌의 TRANSFER_OUT 과 입금 계좌의 TRANSFER_IN 두 줄로 기록된다.
 */
@Entity
@Table(name = "account_tx")
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ACC_TX_SEQ")
    @SequenceGenerator(name = "ACC_TX_SEQ", sequenceName = "account_tx_seq", allocationSize = 1)
    @Column(name = "tx_id")
    private Long id;

    @Column(name = "account_number", nullable = false)
    private Long accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = Account.BALANCE_SCALE)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = Account.BALANCE_SCALE)
    private BigDecimal balanceAfter;

    /** 이체 상대 계좌. 입출금 거래에서는 null. */
    @Column(name = "counterpart_account_number")
    private Long counterpartAccountNumber;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AccountTransaction() {
    }

    private AccountTransaction(Long accountNumber,
                               TransactionType type,
                               BigDecimal amount,
                               BigDecimal balanceAfter,
                               Long counterpartAccountNumber,
                               String description) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.counterpartAccountNumber = counterpartAccountNumber;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public static AccountTransaction of(Account account,
                                        TransactionType type,
                                        BigDecimal amount,
                                        Long counterpartAccountNumber,
                                        String description) {
        return new AccountTransaction(account.getAccountNumber(), type, amount,
                account.getBalance(), counterpartAccountNumber, description);
    }

    public Long getId() {
        return id;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Long getCounterpartAccountNumber() {
        return counterpartAccountNumber;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
