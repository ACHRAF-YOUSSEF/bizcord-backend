package com.bizcord.rtc.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

public record MediasoupSignalMessage(
        @JsonAlias("roomId")
        String channelId,
        String direction,
        String transportId,
        Map<String, Object> dtlsParameters,
        String kind,
        String consumerId,
        String producerId,
        Map<String, Object> rtpParameters,
        Map<String, Object> rtpCapabilities
) {
}

