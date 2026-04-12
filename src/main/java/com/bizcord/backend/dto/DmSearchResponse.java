package com.bizcord.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DmSearchResponse {
    private String id;
    private String content;
    private List<String> attachments;
    private String conversationId;
    private UserItem user;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class UserItem {
        private String id;
        private String fullName;
        private String username;
        private String imageUrl;
    }
}
