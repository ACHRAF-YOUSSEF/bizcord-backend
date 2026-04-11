package com.bizcord.backend.controller;

import com.bizcord.backend.service.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class VoiceWebSocketController {
    private final VoiceService voiceService;

    @MessageMapping("/voice/{channelId}/join")
    @SendToUser("/queue/voice")
    public Map<String, Object> join(
            @DestinationVariable String channelId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> result = voiceService.join(channelId, userDetails.getUsername());
        result.put("action", "joined");
        return result;
    }

    @MessageMapping("/voice/{channelId}/leave")
    public void leave(
            @DestinationVariable String channelId,
            @AuthenticationPrincipal UserDetails userDetails) {
        voiceService.leave(channelId, userDetails.getUsername());
    }

    @MessageMapping("/voice/{channelId}/createTransport")
    @SendToUser("/queue/voice")
    public Map<String, Object> createTransport(
            @DestinationVariable String channelId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> result = voiceService.createTransport(channelId, userDetails.getUsername());
        result.put("action", "transportCreated");
        return result;
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/voice/{channelId}/connectTransport")
    public void connectTransport(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        voiceService.connectTransport(
                channelId,
                (String) payload.get("transportId"),
                payload.get("dtlsParameters"),
                userDetails.getUsername()
        );
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/voice/{channelId}/produce")
    @SendToUser("/queue/voice")
    public Map<String, Object> produce(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> result = voiceService.produce(
                channelId,
                (String) payload.get("transportId"),
                (String) payload.get("kind"),
                payload.get("rtpParameters"),
                (Map<String, Object>) payload.get("appData"),
                userDetails.getUsername()
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
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> result = voiceService.consume(
                channelId,
                (String) payload.get("transportId"),
                (String) payload.get("producerId"),
                payload.get("rtpCapabilities"),
                userDetails.getUsername()
        );
        result.put("action", "consumed");
        return result;
    }

    @MessageMapping("/voice/{channelId}/resumeConsumer")
    public void resumeConsumer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        voiceService.resumeConsumer(channelId, (String) payload.get("consumerId"), userDetails.getUsername());
    }

    @MessageMapping("/voice/{channelId}/pauseProducer")
    public void pauseProducer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        voiceService.pauseProducer(channelId, (String) payload.get("producerId"), userDetails.getUsername());
    }

    @MessageMapping("/voice/{channelId}/resumeProducer")
    public void resumeProducer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        voiceService.resumeProducer(channelId, (String) payload.get("producerId"), userDetails.getUsername());
    }

    @MessageMapping("/voice/{channelId}/closeProducer")
    public void closeProducer(
            @DestinationVariable String channelId,
            Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        voiceService.closeProducer(channelId, (String) payload.get("producerId"), userDetails.getUsername());
    }
}
