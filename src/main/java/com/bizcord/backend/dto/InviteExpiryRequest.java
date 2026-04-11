package com.bizcord.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InviteExpiryRequest {
    private LocalDateTime expiresAt;
}
