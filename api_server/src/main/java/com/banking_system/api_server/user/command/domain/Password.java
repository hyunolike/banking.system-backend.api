package com.banking_system.api_server.user.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Password {
    @Column(name="password")
    private String value;

    protected Password() {}

    public Password(String value){
        this.value = value;
    }

    public boolean match(String password){
        return this.value.equals(password);
    }
}
