package com.bizcord.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactionRequest {
    @NotBlank
    private String emoji;
}
