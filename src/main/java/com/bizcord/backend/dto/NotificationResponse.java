package com.bizcord.backend.dto;

import com.bizcord.backend.entity.NotificationType;
import com.bizcord.backend.entity.ReferenceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String title;
    private String body;
    private String referenceId;
    private ReferenceType referenceType;
    private String serverId;
    private String channelId;
    private boolean read;
    private LocalDateTime createdAt;
}
