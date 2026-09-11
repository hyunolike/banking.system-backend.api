package com.banking_system.api_server.friend.ui;

import com.banking_system.api_server.common.security.CurrentUser;
import com.banking_system.api_server.common.security.LoginUser;
import com.banking_system.api_server.friend.command.application.FriendDtos;
import com.banking_system.api_server.friend.command.application.FriendService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FriendDtos.FriendResponse add(@CurrentUser LoginUser loginUser,
                                         @Valid @RequestBody FriendDtos.CreateFriendRequest request) {
        return friendService.add(loginUser.id(), request);
    }

    @GetMapping
    public List<FriendDtos.FriendResponse> myFriends(@CurrentUser LoginUser loginUser) {
        return friendService.findMyFriends(loginUser.id());
    }

    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser LoginUser loginUser, @PathVariable Long friendId) {
        friendService.delete(loginUser.id(), friendId);
    }
}
