package com.bizcord.backend.dto;

import com.bizcord.backend.entity.ChannelType;
import com.bizcord.backend.entity.MemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ServerResponse {
    private String id;
    private String name;
    private String imageUrl;
    private String inviteCode;
    private String userId;
    private List<MemberItem> members;
    private List<ChannelItem> channels;
    private List<CategoryItem> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class UserItem {
        private String id;
        private String username;
        private String email;
        private String fullName;
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
        private String serverId;
        private UserItem user;
    }

    @Data
    @Builder
    public static class ChannelItem {
        private String id;
        private String name;
        private ChannelType type;
        private String serverId;
        private String userId;
        private String categoryId;
        private int position;
    }

    @Data
    @Builder
    public static class CategoryItem {
        private String id;
        private String name;
        private int position;
        private String serverId;
        private boolean defaultCategory;
    }
}

