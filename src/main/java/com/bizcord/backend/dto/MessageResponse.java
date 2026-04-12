package com.bizcord.backend.dto;

import com.bizcord.backend.entity.MemberRole;
import com.bizcord.backend.entity.UserStatus;
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
    private boolean pinned;
    private LocalDateTime pinnedAt;
    private String pinnedByUsername;
    private List<ReactionGroup> reactions;
    private ParentMessagePreview parentMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class ParentMessagePreview {
        private String id;
        private String content;
        private String senderName;
    }

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

    @Data
    @Builder
    public static class ReactionGroup {
        private String emoji;
        private int count;
        private List<String> userIds;
        private boolean me;
    }
}

