package com.bizcord.backend.dto;

public record TypingEvent(
        String type,
        String userId,
        String username,
        String fullName,
        String imageUrl
) {
}
