package com.bizcord.backend.controller;

import com.bizcord.backend.service.VoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VoiceWebSocketController {
    private final VoiceService voiceService;

    @MessageMapping("/voice/{channelId}/join")
    @SendToUser("/queue/voice")
    public Map<String, Object> join(
            @DestinationVariable String channelId,
            Principal principal) {
        Map<String, Object> result = voiceService.join(channelId, principal.getName());
        result.put("action", "joined");
        return result;
    }

    @MessageMapping("/voice/{channelId}/leave")
    public void leave(
            @DestinationVariable String channelId,
            Principal principal) {
        voiceService.leave(channelId, principal.getName());
    }

    @MessageMapping("/voice/{channelId}/createTransport")
    @SendToUser("/queue/voice")
    public Map<String, Object> createTransport(
            @DestinationVariable String channelId,
            Principal principal) {
        Map<String, Object> result = voiceService.createTransport(channelId, principal.getName());
        result.put("action", "transportCreated");
        return result;
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/voice/{channelId}/connectTransport")
    public void connectTransport(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            Principal principal) {
        voiceService.connectTransport(
                channelId,
                (String) payload.get("transportId"),
                payload.get("dtlsParameters"),
                principal.getName()
        );
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/voice/{channelId}/produce")
    @SendToUser("/queue/voice")
    public Map<String, Object> produce(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            Principal principal) {
        Map<String, Object> result = voiceService.produce(
                channelId,
                (String) payload.get("transportId"),
                (String) payload.get("kind"),
                payload.get("rtpParameters"),
                (Map<String, Object>) payload.get("appData"),
                principal.getName()
        );
        result.put("action", "produced");
        return result;
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/voice/{channelId}/consume")
    @SendToUser("/queue/voice")
    public Map<String, Object> consume(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            Principal principal) {
        Map<String, Object> result = voiceService.consume(
                channelId,
                (String) payload.get("transportId"),
                (String) payload.get("producerId"),
                payload.get("rtpCapabilities"),
                principal.getName()
        );
        result.put("action", "consumed");
        return result;
    }

    @MessageMapping("/voice/{channelId}/resumeConsumer")
    public void resumeConsumer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            Principal principal) {
        voiceService.resumeConsumer(channelId, (String) payload.get("consumerId"), principal.getName());
    }

    @MessageMapping("/voice/{channelId}/pauseProducer")
    public void pauseProducer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            Principal principal) {
        voiceService.pauseProducer(channelId, (String) payload.get("producerId"), principal.getName());
    }

    @MessageMapping("/voice/{channelId}/resumeProducer")
    public void resumeProducer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            Principal principal) {
        voiceService.resumeProducer(channelId, (String) payload.get("producerId"), principal.getName());
    }

    @MessageMapping("/voice/{channelId}/closeProducer")
    public void closeProducer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            Principal principal) {
        voiceService.closeProducer(channelId, (String) payload.get("producerId"), principal.getName());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/voice")
    public Map<String, Object> handleException(Exception e) {
        log.error("Voice WebSocket error: {}", e.getMessage());
        return Map.of("action", "error", "message", e.getMessage() != null ? e.getMessage() : "Unknown error");
    }
}
