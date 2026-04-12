package com.bizcord.backend.dto;

import com.bizcord.backend.entity.AttendeeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventRsvpRequest {
    @NotNull
    private AttendeeStatus status;
}
