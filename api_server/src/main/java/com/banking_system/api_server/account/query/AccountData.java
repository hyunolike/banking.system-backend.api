package com.banking_system.api_server.account.query;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "account")
public class AccountData {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ACC_SEQ")
    @SequenceGenerator(sequenceName = "account_seq", allocationSize = 1, name="ACC_SEQ")
    @Column(name = "account_number")
    private Long id;
    @Column(name = "account_id")
    private Long user_id;
    @Column(name = "name")
    private String name;
    @Column(name = "balance")
    private String balance;
}
