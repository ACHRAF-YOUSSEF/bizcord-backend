package com.bizcord.backend.dto;

import com.bizcord.backend.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull UserStatus status) {
}
