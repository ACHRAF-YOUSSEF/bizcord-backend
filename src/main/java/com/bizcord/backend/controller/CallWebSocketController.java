package com.bizcord.backend.controller;

import com.bizcord.backend.service.CallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CallWebSocketController {
    private final CallService callService;

    @MessageMapping("/calls/{conversationId}/initiate")
    public void initiateCall(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        boolean withVideo = Boolean.TRUE.equals(payload.get("withVideo"));
        callService.initiateCall(conversationId, withVideo, principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/accept")
    public void acceptCall(
            @DestinationVariable String conversationId,
            Principal principal) {
        callService.acceptCall(conversationId, principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/decline")
    public void declineCall(
            @DestinationVariable String conversationId,
            Principal principal) {
        callService.declineCall(conversationId, principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/end")
    public void endCall(
            @DestinationVariable String conversationId,
            Principal principal) {
        callService.endCall(conversationId, principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/join")
    @SendToUser("/queue/calls")
    public Map<String, Object> join(
            @DestinationVariable String conversationId,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> result = callService.join(conversationId, principal.getName(), headerAccessor.getSessionId());
        result.put("action", "joined");
        return result;
    }

    @MessageMapping("/calls/{conversationId}/leave")
    public void leave(
            @DestinationVariable String conversationId,
            Principal principal) {
        callService.leave(conversationId, principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/createTransport")
    @SendToUser("/queue/calls")
    public Map<String, Object> createTransport(
            @DestinationVariable String conversationId,
            Principal principal) {
        Map<String, Object> result = callService.createTransport(conversationId, principal.getName());
        result.put("action", "transportCreated");
        return result;
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/calls/{conversationId}/connectTransport")
    public void connectTransport(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        callService.connectTransport(
                conversationId,
                (String) payload.get("transportId"),
                payload.get("dtlsParameters"),
                principal.getName()
        );
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/calls/{conversationId}/produce")
    @SendToUser("/queue/calls")
    public Map<String, Object> produce(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        Map<String, Object> result = callService.produce(
                conversationId,
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
    @MessageMapping("/calls/{conversationId}/consume")
    @SendToUser("/queue/calls")
    public Map<String, Object> consume(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        Map<String, Object> result = callService.consume(
                conversationId,
                (String) payload.get("transportId"),
                (String) payload.get("producerId"),
                payload.get("rtpCapabilities"),
                principal.getName()
        );
        result.put("action", "consumed");
        return result;
    }

    @MessageMapping("/calls/{conversationId}/resumeConsumer")
    public void resumeConsumer(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        callService.resumeConsumer(conversationId, (String) payload.get("consumerId"), principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/pauseProducer")
    public void pauseProducer(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        callService.pauseProducer(conversationId, (String) payload.get("producerId"), principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/resumeProducer")
    public void resumeProducer(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        callService.resumeProducer(conversationId, (String) payload.get("producerId"), principal.getName());
    }

    @MessageMapping("/calls/{conversationId}/closeProducer")
    public void closeProducer(
            @DestinationVariable String conversationId,
            Map<String, Object> payload,
            Principal principal) {
        callService.closeProducer(conversationId, (String) payload.get("producerId"), principal.getName());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/calls")
    public Map<String, Object> handleException(Exception e) {
        log.error("Call WebSocket error: {}", e.getMessage());
        return Map.of("action", "error", "message", e.getMessage() != null ? e.getMessage() : "Unknown error");
    }
}
