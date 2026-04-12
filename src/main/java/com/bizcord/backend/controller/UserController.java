package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.dto.UserStatusRequest;
import com.bizcord.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUser(userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/friends")
    public ResponseEntity<List<UserProfileResponse>> getFriends(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getFriends(userDetails.getUsername()));
    }

    @RateLimit(limit = 60, keyType = RateLimitKeyType.UID)
    @PatchMapping("/me/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody UserStatusRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        userService.updateStatus(userDetails.getUsername(), request.getStatus());
        return ResponseEntity.noContent().build();
    }
}

