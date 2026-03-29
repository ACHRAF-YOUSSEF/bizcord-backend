package com.bizcord.backend.dto;

public record TokenPair(String accessToken, String refreshToken, long expiresIn) {}

