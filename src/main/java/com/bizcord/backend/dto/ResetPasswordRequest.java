package com.bizcord.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit or special character"
        )
        String newPassword
) {}
