package com.bizcord.backend.dto;

public record AuthResponse(String token, long expiresIn) {
}
