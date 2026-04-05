package com.bizcord.backend.dto;

import com.bizcord.backend.entity.MemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private String id;
    private MemberItem member1;
    private MemberItem member2;
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

    @Data
    @Builder
    public static class MemberItem {
        private String id;
        private String name;
        private MemberRole role;
        private UserItem user;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}

