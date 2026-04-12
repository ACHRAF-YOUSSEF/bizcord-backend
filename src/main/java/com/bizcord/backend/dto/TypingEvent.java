package com.bizcord.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TypingEvent {
    private String type; // "TYPING_START" or "TYPING_STOP"
    private String userId;
    private String username;
    private String fullName;
    private String imageUrl;
}
