package com.banking_system.api_server.friend.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    List<Friend> findAllByUserIdOrderByNameAsc(Long userId);
}
