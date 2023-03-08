package com.banking_system.api_server.account.command.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import jakarta.persistence.*;
@Entity
@Table(name="account")
public class Account {
    @EmbeddedId
    private AccountNo accountNo;

    // user 테이블의 id와 맵핑
    @Embedded
    private AccountUser accountUser;

    private int balance;
}
