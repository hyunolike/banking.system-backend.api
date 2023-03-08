package com.banking_system.api_server.account.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Embeddable
public class AccountNo implements Serializable {
    @Column(name="account_number")
    private Long number;

    protected AccountNo() {}

    public AccountNo(Long number){
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountNo accountNo = (AccountNo) o;
        return Objects.equals(number, accountNo.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    public static AccountNo of(Long number){
        return new AccountNo(number);
    }
}
