package com.bizcord.backend.dto;

import com.bizcord.backend.entity.AttendeeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EventResponse {
    private String id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String color;
    private String serverId;
    private CreatorItem creator;
    private List<AttendeeItem> attendees;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class CreatorItem {
        private String id;
        private String fullName;
        private String username;
        private String imageUrl;
    }

    @Data
    @Builder
    public static class AttendeeItem {
        private String id;
        private String userId;
        private String fullName;
        private String username;
        private String imageUrl;
        private AttendeeStatus status;
    }
}
