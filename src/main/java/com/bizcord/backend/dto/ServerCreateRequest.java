package com.bizcord.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServerCreateRequest {
    @NotBlank
    private String name;

    private String imageUrl;
}

