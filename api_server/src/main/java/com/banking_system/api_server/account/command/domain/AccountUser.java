package com.banking_system.api_server.account.command.domain;

import com.banking_system.api_server.user.command.domain.UserId;
import jakarta.persistence.*;

@Embeddable
public class AccountUser {

    @Embedded
    @AttributeOverrides(
            @AttributeOverride(name="id", column = @Column(name="account_id"))
    )
    private UserId userId;

    // 계좌 번호
    @Column(name="account_name")
    private String name;
}
