package com.bizcord.rtc.dto;

import java.util.Map;

public record RtcIceCandidateSignalMessage(
        String fromUserId,
        String targetUserId,
        String roomId,
        Map<String, Object> candidate
) {
}

