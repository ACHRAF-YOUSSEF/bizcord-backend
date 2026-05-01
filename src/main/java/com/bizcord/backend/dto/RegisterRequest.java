package com.bizcord.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Size(min = 3, max = 20) @NotBlank String fullName,
        @Size(min = 3, max = 15) @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit or special character"
        )
        String password
) {
}
