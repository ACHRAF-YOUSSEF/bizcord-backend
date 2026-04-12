package com.bizcord.rtc.dto;

import com.bizcord.rtc.model.CallStatus;
import com.bizcord.rtc.model.CallType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CallHistoryResponse(
        UUID id,
        String roomId,
        CallType callType,
        String otherPartyId,
        LocalDateTime startedAt,
        Long durationSeconds,
        CallStatus status
) {}

