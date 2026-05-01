package com.bizcord.backend.dto;

import com.bizcord.backend.entity.FriendshipStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendshipResponse {
    private String id;
    private UserProfileResponse requester;
    private UserProfileResponse addressee;
    private FriendshipStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

