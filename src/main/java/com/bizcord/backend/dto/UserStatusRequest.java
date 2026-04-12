package com.bizcord.backend.dto;

import com.bizcord.backend.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {
    @NotNull
    private UserStatus status;
}
