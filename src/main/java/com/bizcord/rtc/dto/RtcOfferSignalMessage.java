package com.bizcord.rtc.dto;

import java.util.Map;

public record RtcOfferSignalMessage(
        String callerId,
        String targetUserId,
        String roomId,
        Map<String, Object> offer
) {
}

