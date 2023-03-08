package com.banking_system.api_server.user.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
public class UserId implements Serializable {
    @Column(name="user_id")
    private Long id;

    protected UserId() {}

    public UserId(Long id){
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(id, userId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static UserId of(Long id){
        return new UserId(id);
    }
}
