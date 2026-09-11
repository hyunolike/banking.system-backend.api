package com.banking_system.api_server.friend.command.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FriendDtos {

    private FriendDtos() {
    }

    public record CreateFriendRequest(
            @NotBlank(message = "친구 이름은 필수입니다.")
            @Size(max = 50, message = "친구 이름은 50자 이하여야 합니다.")
            String name,

            Long accountNumber) {
    }

    public record FriendResponse(Long id, String name, Long accountNumber) {
    }
}
