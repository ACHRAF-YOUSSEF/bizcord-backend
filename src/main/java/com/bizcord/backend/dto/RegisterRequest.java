package com.bizcord.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @Size(min = 3, max = 20)
    @NotBlank
    private String fullName;

    @Size(min = 3, max = 15)
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @Size(min = 8)
    @NotBlank
    private String password;
}
