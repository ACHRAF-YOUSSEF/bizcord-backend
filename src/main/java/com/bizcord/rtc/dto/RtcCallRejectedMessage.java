package com.bizcord.rtc.dto;

public record RtcCallRejectedMessage(
        String rejectorId,
        String roomId
) {
}

