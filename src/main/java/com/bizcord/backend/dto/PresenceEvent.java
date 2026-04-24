package com.bizcord.backend.dto;

import com.bizcord.backend.entity.UserStatus;

public record PresenceEvent(String type, String userId, UserStatus status) {
}
