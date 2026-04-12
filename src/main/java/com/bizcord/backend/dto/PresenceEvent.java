package com.bizcord.backend.dto;

import com.bizcord.backend.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PresenceEvent {
    private String type;
    private String userId;
    private UserStatus status;
}
