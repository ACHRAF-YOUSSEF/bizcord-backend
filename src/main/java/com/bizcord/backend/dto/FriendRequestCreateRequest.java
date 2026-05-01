package com.bizcord.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record FriendRequestCreateRequest(@NotBlank String username) {
}

