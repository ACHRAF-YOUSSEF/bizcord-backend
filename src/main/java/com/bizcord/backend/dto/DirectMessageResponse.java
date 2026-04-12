package com.bizcord.backend.dto;

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
    private UserItem user;
    private String conversationId;
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
        private String email;
        private String imageUrl;
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

