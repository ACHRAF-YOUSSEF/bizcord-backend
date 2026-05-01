package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.PasswordChangeRequest;
import com.bizcord.backend.dto.UserProfileResponse;
import com.bizcord.backend.dto.UserProfileUpdateRequest;
import com.bizcord.backend.dto.UserStatusRequest;
import com.bizcord.backend.service.UploadService;
import com.bizcord.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UploadService uploadService;

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
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(@RequestParam("q") String query,
                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.searchUsers(query, userDetails.getUsername()));
    }

    @RateLimit(limit = 60, keyType = RateLimitKeyType.UID)
    @PatchMapping("/me/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody UserStatusRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        userService.updateStatus(userDetails.getUsername(), request.status());
        return ResponseEntity.noContent().build();
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }

    @RateLimit(limit = 5, keyType = RateLimitKeyType.UID)
    @PatchMapping("/me/avatar")
    public ResponseEntity<UserProfileResponse> updateAvatar(@RequestParam("file") MultipartFile file,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        var upload = uploadService.uploadImage(file);
        return ResponseEntity.ok(userService.updateAvatar(userDetails.getUsername(), upload.url()));
    }

    @RateLimit(limit = 5, keyType = RateLimitKeyType.UID)
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @RateLimit(limit = 3, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAccount(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
