package com.banking_system.api_server.friend.command.application;

import com.banking_system.api_server.common.error.BusinessException;
import com.banking_system.api_server.common.error.ErrorCode;
import com.banking_system.api_server.friend.command.domain.Friend;
import com.banking_system.api_server.friend.command.domain.FriendRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FriendService {

    private final FriendRepository friendRepository;

    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    @Transactional
    public FriendDtos.FriendResponse add(Long userId, FriendDtos.CreateFriendRequest request) {
        Friend friend = friendRepository.save(
                Friend.of(userId, request.name(), request.accountNumber()));
        return toResponse(friend);
    }

    public List<FriendDtos.FriendResponse> findMyFriends(Long userId) {
        return friendRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(FriendService::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_NOT_FOUND));
        friend.requireOwner(userId);
        friendRepository.delete(friend);
    }

    private static FriendDtos.FriendResponse toResponse(Friend friend) {
        return new FriendDtos.FriendResponse(friend.getId(), friend.getName(), friend.getAccountNumber());
    }
}
