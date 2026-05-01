package com.bizcord.backend.dto;

import com.bizcord.backend.entity.ConversationRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private String id;
    private UserItem user1;
    private UserItem user2;
    private ConversationRequestStatus requestStatus;
    private String requesterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class UserItem {
        private String id;
        private String fullName;
        private String username;
        private String email;
        private String imageUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
