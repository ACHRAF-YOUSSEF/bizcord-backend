package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.FriendRequestCreateRequest;
import com.bizcord.backend.dto.FriendshipResponse;
import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {
    private final FriendshipService friendshipService;

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getFriends(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendshipService.getFriends(userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/requests/incoming")
    public ResponseEntity<List<FriendshipResponse>> getIncomingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendshipService.getIncomingRequests(userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<FriendshipResponse>> getOutgoingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendshipService.getOutgoingRequests(userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PostMapping("/requests")
    public ResponseEntity<FriendshipResponse> sendRequest(@Valid @RequestBody FriendRequestCreateRequest request,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(CREATED)
                .body(friendshipService.sendRequest(request.username(), userDetails.getUsername()));
    }

    @RateLimit(limit = 20, keyType = RateLimitKeyType.UID)
    @PatchMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendshipResponse> acceptRequest(@PathVariable String requestId,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendshipService.acceptRequest(requestId, userDetails.getUsername()));
    }

    @RateLimit(limit = 20, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> deleteRequestOrFriend(@PathVariable String requestId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        friendshipService.deleteRequestOrFriend(requestId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

