package com.banking_system.api_server.friend.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;

/**
 * 이체 상대를 빠르게 고르기 위한 즐겨찾기. 사용자별로 이름과 계좌번호를 묶어 보관한다.
 */
@Entity
@Table(name = "friend")
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FRI_SEQ")
    @SequenceGenerator(name = "FRI_SEQ", sequenceName = "friend_seq", allocationSize = 1)
    @Column(name = "friend_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 즐겨찾기한 상대 계좌번호. 아직 계좌를 모르면 null. */
    @Column(name = "account_number")
    private Long accountNumber;

    protected Friend() {
    }

    private Friend(Long userId, String name, Long accountNumber) {
        this.userId = userId;
        this.name = name;
        this.accountNumber = accountNumber;
    }

    public static Friend of(Long userId, String name, Long accountNumber) {
        return new Friend(userId, name, accountNumber);
    }

    public void requireOwner(Long candidateUserId) {
        if (!this.userId.equals(candidateUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }
}
