package com.banking_system.api_server.friend.query;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="friend")
public class FriendData {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FRI_SEQ")
    @SequenceGenerator(sequenceName = "friend_seq", allocationSize = 1, name="FRI_SEQ")
    @Column(name = "friend_id")
    private Long id;
    @Column(name = "user_id")
    private Long user_id;
    @Column(name = "name")
    private String name;
}
