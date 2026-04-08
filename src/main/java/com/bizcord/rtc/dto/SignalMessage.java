package com.bizcord.rtc.dto;

public record SignalMessage(
        String targetUserId,
        String callerId,
        String roomId,
        String sdp,
        String candidate
) {
}

