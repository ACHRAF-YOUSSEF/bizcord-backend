package com.bizcord.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
}
