package com.bizcord.backend.dto;

import com.bizcord.backend.entity.MemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageResponse {
    private String id;
    private String content;
    private List<String> attachments;
    private MemberItem member;
    private String channelId;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class UserItem {
        private String id;
        private String fullName;
        private String username;
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

