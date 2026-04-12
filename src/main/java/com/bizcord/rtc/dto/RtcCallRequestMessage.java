package com.bizcord.rtc.dto;

public record RtcCallRequestMessage(
        String callerId,
        String callerName,
        String callerAvatarUrl,
        String callType,
        String roomId
) {
}

