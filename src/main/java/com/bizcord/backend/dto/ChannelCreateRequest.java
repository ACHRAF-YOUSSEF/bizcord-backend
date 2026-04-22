package com.bizcord.backend.dto;

import com.bizcord.backend.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelCreateRequest {
    @NotBlank
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[^<>\"'`]*$", message = "Channel name contains unsupported characters")
    private String name;

    @NotNull
    private ChannelType type;

    private String categoryId;
}
