package com.bizcord.rtc.controller;

import com.bizcord.rtc.dto.SignalMessage;
import com.bizcord.rtc.model.CallType;
import com.bizcord.rtc.service.CallRecordService;
import com.bizcord.rtc.service.MediasoupSidecarService;
import com.bizcord.rtc.service.RtcRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RtcSignalingController {
    private final SimpMessagingTemplate messagingTemplate;
    private final RtcRoomService rtcRoomService;
    private final CallRecordService callRecordService;
    private final MediasoupSidecarService mediasoupSidecarService;

    @MessageMapping("/rtc/call-request")
    public void handleCallRequest(
            @Payload SignalMessage message,
            Principal principal) {

        String callerId = principal.getName();
        log.debug("call-request from {} to {} (room={})", callerId, message.targetUserId(), message.roomId());

        if (message.roomId() != null) {
            rtcRoomService.joinRoom(message.roomId(), callerId);
        }

        messagingTemplate.convertAndSendToUser(
                message.targetUserId(),
                "/queue/rtc/call-request",
                new SignalMessage(
                        message.targetUserId(),
                        callerId,
                        message.roomId(),
                        null,
                        null));
    }

    @MessageMapping("/rtc/reject-call")
    public void handleRejectCall(
            @Payload SignalMessage message,
            Principal principal) {

        String rejecterId = principal.getName();
        log.debug("reject-call from {} to {} (room={})", rejecterId, message.callerId(), message.roomId());

        if (message.roomId() != null) {
            rtcRoomService.leaveRoom(message.roomId(), rejecterId);
            if (message.callerId() != null) {
                rtcRoomService.leaveRoom(message.roomId(), message.callerId());
            }
        }

        if (message.callerId() != null) {
            messagingTemplate.convertAndSendToUser(
                    message.callerId(),
                    "/queue/rtc/call-rejected",
                    new SignalMessage(
                            message.callerId(),
                            rejecterId,
                            message.roomId(),
                            null,
                            null));
        }
    }

    @MessageMapping("/rtc/end-call")
    public void handleEndCall(
            @Payload SignalMessage message,
            Principal principal) {

        String hangUpUserId = principal.getName();
        String otherUserId = message.targetUserId();
        log.debug("end-call from {} (room={})", hangUpUserId, message.roomId());

        if (message.roomId() != null) {
            rtcRoomService.leaveRoom(message.roomId(), hangUpUserId);
            if (otherUserId != null) {
                rtcRoomService.leaveRoom(message.roomId(), otherUserId);
            }
        }

        if (otherUserId != null) {
            messagingTemplate.convertAndSendToUser(
                    otherUserId,
                    "/queue/rtc/end-call",
                    new SignalMessage(
                            otherUserId,
                            hangUpUserId,
                            message.roomId(),
                            null,
                            null));
        }
    }

    @MessageMapping("/rtc/offer")
    public void handleOffer(
            @Payload SignalMessage message,
            Principal principal) {

        String senderId = principal.getName();
        log.debug("SDP offer from {} to {} (room={})", senderId, message.targetUserId(), message.roomId());

        messagingTemplate.convertAndSendToUser(
                message.targetUserId(),
                "/queue/rtc/offer",
                new SignalMessage(
                        message.targetUserId(),
                        senderId,
                        message.roomId(),
                        message.sdp(),
                        null));
    }

    @MessageMapping("/rtc/answer")
    public void handleAnswer(
            @Payload SignalMessage message,
            Principal principal) {

        String answererId = principal.getName();
        log.debug("SDP answer from {} to {} (room={})", answererId, message.callerId(), message.roomId());

        if (message.roomId() != null) {
            rtcRoomService.joinRoom(message.roomId(), answererId);
        }

        if (message.callerId() != null && message.roomId() != null) {
            callRecordService.createCallRecord(
                    message.roomId(),
                    message.callerId(),
                    answererId,
                    CallType.DM);
        }

        if (message.callerId() != null) {
            messagingTemplate.convertAndSendToUser(
                    message.callerId(),
                    "/queue/rtc/answer",
                    new SignalMessage(
                            message.callerId(),
                            answererId,
                            message.roomId(),
                            message.sdp(),
                            null));
        }
    }

    @MessageMapping("/rtc/ice-candidate")
    public void handleIceCandidate(
            @Payload SignalMessage message,
            Principal principal) {

        String senderId = principal.getName();

        messagingTemplate.convertAndSendToUser(
                message.targetUserId(),
                "/queue/rtc/ice-candidate",
                new SignalMessage(
                        message.targetUserId(),
                        senderId,
                        message.roomId(),
                        null,
                        message.candidate()));
    }

    @MessageMapping("/rtc/ms-get-caps")
    public void handleGetRouterCaps(
            @Payload SignalMessage message,
            Principal principal) {

        String userId = principal.getName();

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-get-caps requested by {} but mediasoup sidecar is unavailable", userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-router-caps",
                    Map.of("error", "mediasoup_unavailable"));
            return;
        }

        Map<String, Object> caps = mediasoupSidecarService.getRouterCapabilities(message.roomId());
        if (caps != null) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-router-caps", caps);
        } else {
            log.warn("ms-get-caps: sidecar returned null for roomId={} user={}", message.roomId(), userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-router-caps",
                    Map.of("error", "sidecar_error"));
        }
    }

    @MessageMapping("/rtc/ms-create-transport")
    public void handleCreateTransport(
            @Payload SignalMessage message,
            Principal principal) {

        String userId = principal.getName();

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-create-transport requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String direction = message.candidate() != null ? message.candidate() : "send";
        Map<String, Object> transportParams =
                mediasoupSidecarService.createWebRtcTransport(message.roomId(), direction);

        if (transportParams != null) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-transport-params", transportParams);
        }
    }

    @MessageMapping("/rtc/ms-connect-transport")
    @SuppressWarnings("unchecked")
    public void handleConnectTransport(
            @Payload SignalMessage message,
            Principal principal) {

        String userId = principal.getName();

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-connect-transport requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String transportId = message.targetUserId();
        Map<String, Object> dtlsParams = Map.of("raw", message.sdp() != null ? message.sdp() : "");

        mediasoupSidecarService.connectTransport(message.roomId(), transportId, dtlsParams);
        log.debug("ms-connect-transport for room={} transport={} user={}", message.roomId(), transportId, userId);
    }

    @MessageMapping("/rtc/ms-produce")
    public void handleProduce(
            @Payload SignalMessage message,
            Principal principal) {

        String userId = principal.getName();

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-produce requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String transportId = message.targetUserId();
        String kind = message.candidate() != null ? message.candidate() : "audio";
        Map<String, Object> rtpParameters = Map.of("raw", message.sdp() != null ? message.sdp() : "");

        Map<String, Object> result =
                mediasoupSidecarService.produce(message.roomId(), transportId, kind, rtpParameters);

        if (result != null) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-producer-id", result);
        }
    }

    @MessageMapping("/rtc/ms-consume")
    public void handleConsume(
            @Payload SignalMessage message,
            Principal principal) {

        String userId = principal.getName();

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-consume requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String transportId = message.targetUserId();
        String producerId = message.callerId();
        Map<String, Object> rtpCapabilities = Map.of("raw", message.sdp() != null ? message.sdp() : "");

        Map<String, Object> result =
                mediasoupSidecarService.consume(message.roomId(), transportId, producerId, rtpCapabilities);

        if (result != null) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-new-consumer", result);
        }
    }
}

