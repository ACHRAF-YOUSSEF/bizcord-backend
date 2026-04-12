package com.bizcord.backend.dto;

import com.bizcord.backend.entity.MemberRole;
import com.bizcord.backend.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageSearchResponse {
    private String id;
    private String content;
    private List<String> attachments;
    private String channelId;
    private String channelName;
    private MemberItem member;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class UserItem {
        private String id;
        private String fullName;
        private String username;
        private String imageUrl;
        private UserStatus status;
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
