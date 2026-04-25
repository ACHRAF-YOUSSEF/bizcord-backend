package com.bizcord.backend.dto;

import com.bizcord.backend.entity.UserStatus;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String id,
        String fullName,
        String username,
        String email,
        String imageUrl,
        UserStatus status,
        UserStatus preferredStatus,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
