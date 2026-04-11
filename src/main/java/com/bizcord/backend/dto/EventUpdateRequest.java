package com.bizcord.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventUpdateRequest {
    @Size(min = 1, max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Size(max = 200)
    private String location;

    @Size(max = 7)
    private String color;
}
