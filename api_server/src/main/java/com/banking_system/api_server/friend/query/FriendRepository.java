package com.banking_system.api_server.friend.query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface FriendRepository extends JpaRepository<FriendData, Long> {
}
