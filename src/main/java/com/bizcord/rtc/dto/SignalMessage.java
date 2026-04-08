package com.bizcord.rtc.dto;

import java.util.Map;

public record SignalMessage(
        String targetUserId,
        String callerId,
        String roomId,
        String sdp,
        String candidate,
        Map<String, Object> data
) {
}

