package com.banking_system.api_server.user.command.domain;

import com.banking_system.api_server.account.command.domain.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@AllArgsConstructor
@Getter
@Entity
@Table(name = "member")
public class User {
    @EmbeddedId
    private UserId id;

    private String name;
    @Embedded
    private Password password;

    private String email;

    @OneToMany(
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JoinColumn(name="account_number")
    private List<Account> accounts = new ArrayList<>();


    public User() {

    }

//    public void initializePassword() {
//        String newPassword = generateRandomPassword();
//        this.password = new Password(newPassword);
//
//    }

    private String generateRandomPassword() {
        Random random = new Random();
        int number = random.nextInt();
        return Integer.toHexString(number);
    }
}
