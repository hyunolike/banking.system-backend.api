package com.banking_system.api_server.account.command.domain;

import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌 애그리거트. 잔액 변경은 전부 이 클래스의 메서드를 통해서만 일어난다.
 *
 * <p>금액은 부동소수 오차가 없어야 하므로 {@link BigDecimal} 을 쓴다.
 * (이전 구현은 조회 엔티티가 {@code String}, 커맨드 엔티티가 {@code int} 였다.)</p>
 */
@Entity
@Table(name = "account")
public class Account {

    /** 잔액 스케일. 원화 기준이지만 이자·수수료 계산 여지를 위해 소수점 4자리를 둔다. */
    public static final int BALANCE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ACC_SEQ")
    @SequenceGenerator(name = "ACC_SEQ", sequenceName = "account_seq", allocationSize = 1)
    @Column(name = "account_number")
    private Long accountNumber;

    /** member.user_id 참조. 애그리거트 경계를 넘지 않기 위해 ID 로만 참조한다. */
    @Column(name = "account_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "balance", nullable = false, precision = 19, scale = BALANCE_SCALE)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Account() {
    }

    private Account(Long userId, String name, BigDecimal initialBalance) {
        this.userId = userId;
        this.name = name;
        this.balance = scaled(initialBalance);
        this.createdAt = LocalDateTime.now();
    }

    public static Account open(Long userId, String name, BigDecimal initialBalance) {
        BigDecimal seed = initialBalance == null ? BigDecimal.ZERO : initialBalance;
        if (seed.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "최초 입금액은 0 이상이어야 합니다.");
        }
        requireSupportedScale(seed);
        return new Account(userId, name, seed);
    }

    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        this.balance = scaled(this.balance.add(amount));
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        BigDecimal next = this.balance.subtract(amount);
        if (next.signum() < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = scaled(next);
    }

    public boolean isOwnedBy(Long candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    public void requireOwner(Long candidateUserId) {
        if (!isOwnedBy(candidateUserId)) {
            throw new BusinessException(ErrorCode.NOT_ACCOUNT_OWNER);
        }
    }

    public void rename(String newName) {
        this.name = newName;
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        requireSupportedScale(amount);
    }

    /** 스케일 초과 금액은 반올림으로 삼키지 않고 400 으로 되돌려준다. */
    private static void requireSupportedScale(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > BALANCE_SCALE) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT,
                    "금액은 소수점 " + BALANCE_SCALE + "자리까지만 입력할 수 있습니다.");
        }
    }

    private static BigDecimal scaled(BigDecimal value) {
        return value.setScale(BALANCE_SCALE, java.math.RoundingMode.UNNECESSARY);
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
