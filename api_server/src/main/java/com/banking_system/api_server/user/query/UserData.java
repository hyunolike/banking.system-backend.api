package com.banking_system.api_server.user.query;

import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name="member")
public class UserData {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_SEQ")
    @SequenceGenerator(sequenceName = "user_seq", allocationSize = 1, name="USER_SEQ")
    @Column(name="user_id")
    private Long id;
    @Column(name="name")
    private String name;
    @Column(name="email")
    private String email;
    @Column(name="password")
    private String password;
//    @OneToMany(
//            cascade = CascadeType.REMOVE,
//            orphanRemoval = true,
//            fetch = FetchType.EAGER
//    )
//    @JoinColumn(name="account_number")
//    private List<Account> accounts = new ArrayList<>();

    public UserData() {

    }
}
