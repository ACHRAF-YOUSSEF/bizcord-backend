package com.bizcord.backend.dto;

import com.bizcord.backend.entity.ChannelType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelUpdateRequest {
    @Size(min = 1, max = 100)
    private String name;

    private ChannelType type;
}

