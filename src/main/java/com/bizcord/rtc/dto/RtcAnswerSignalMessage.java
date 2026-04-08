package com.bizcord.rtc.dto;

import java.util.Map;

public record RtcAnswerSignalMessage(
        String answererId,
        String targetUserId,
        String roomId,
        Map<String, Object> answer
) {
}

