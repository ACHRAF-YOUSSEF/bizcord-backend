package com.bizcord.backend.service;

import com.bizcord.backend.dto.FriendshipResponse;
import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.entity.Friendship;
import com.bizcord.backend.entity.FriendshipStatus;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.mapper.UserProfileMapper;
import com.bizcord.backend.repository.FriendshipRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getFriends(String email) {
        User currentUser = getCurrentUser(email);
        return friendshipRepository.findAllByUserIdAndStatus(currentUser.getId(), FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> userProfileMapper.toResponse(otherUser(friendship, currentUser)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> getIncomingRequests(String email) {
        return friendshipRepository.findIncomingRequests(getCurrentUser(email).getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> getOutgoingRequests(String email) {
        return friendshipRepository.findOutgoingRequests(getCurrentUser(email).getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FriendshipResponse sendRequest(String username, String email) {
        User currentUser = getCurrentUser(email);
        User targetUser = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.USER_NOT_FOUND));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.FRIEND_SELF_NOT_ALLOWED);
        }

        return friendshipRepository.findBetweenUsers(currentUser.getId(), targetUser.getId())
                .map(friendship -> {
                    if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorMessages.FRIEND_ALREADY_EXISTS);
                    }
                    if (friendship.getAddressee().getId().equals(currentUser.getId())) {
                        friendship.setStatus(FriendshipStatus.ACCEPTED);
                    }
                    return toResponse(friendshipRepository.save(friendship));
                })
                .orElseGet(() -> toResponse(friendshipRepository.save(Friendship.builder()
                        .requester(currentUser)
                        .addressee(targetUser)
                        .status(FriendshipStatus.PENDING)
                        .build())));
    }

    @Transactional
    public FriendshipResponse acceptRequest(String requestId, String email) {
        User currentUser = getCurrentUser(email);
        Friendship friendship = getRequest(requestId);
        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.FRIEND_REQUEST_FORBIDDEN);
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return toResponse(friendshipRepository.save(friendship));
    }

    @Transactional
    public void deleteRequestOrFriend(String requestId, String email) {
        User currentUser = getCurrentUser(email);
        Friendship friendship = getRequest(requestId);
        boolean involved = friendship.getRequester().getId().equals(currentUser.getId())
                || friendship.getAddressee().getId().equals(currentUser.getId());
        if (!involved) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.FRIEND_REQUEST_FORBIDDEN);
        }
        friendshipRepository.delete(friendship);
    }

    public boolean areFriends(String userId1, String userId2) {
        return friendshipRepository.findBetweenUsers(userId1, userId2)
                .filter(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
                .isPresent();
    }

    private Friendship getRequest(String requestId) {
        return friendshipRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.FRIEND_REQUEST_NOT_FOUND));
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private User otherUser(Friendship friendship, User currentUser) {
        return friendship.getRequester().getId().equals(currentUser.getId())
                ? friendship.getAddressee()
                : friendship.getRequester();
    }

    private FriendshipResponse toResponse(Friendship friendship) {
        return FriendshipResponse.builder()
                .id(friendship.getId())
                .requester(userProfileMapper.toResponse(friendship.getRequester()))
                .addressee(userProfileMapper.toResponse(friendship.getAddressee()))
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .updatedAt(friendship.getUpdatedAt())
                .build();
    }
}

