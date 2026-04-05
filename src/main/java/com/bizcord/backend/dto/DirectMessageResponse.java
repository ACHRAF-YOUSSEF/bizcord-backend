package com.bizcord.backend.dto;

import com.bizcord.backend.entity.MemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DirectMessageResponse {
    private String id;
    private String content;
    private List<String> attachments;
    private MemberItem member;
    private String conversationId;
    private boolean deleted;
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
    }

    @Data
    @Builder
    public static class MemberItem {
        private String id;
        private String name;
        private MemberRole role;
        private UserItem user;
    }
}

