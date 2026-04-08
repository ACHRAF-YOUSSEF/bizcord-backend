package com.bizcord.rtc.dto;

public record RtcCallEndedMessage(
        String endedByUserId,
        String roomId
) {
}

