package com.bizcord.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Size(min = 3, max = 20) @NotBlank String fullName,
        @Size(min = 3, max = 15) @NotBlank String username,
        @NotBlank @Email String email,
        @Size(min = 8) @NotBlank String password
) {
}
