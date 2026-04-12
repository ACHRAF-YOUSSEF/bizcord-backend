package com.bizcord.rtc.controller;

import com.bizcord.backend.entity.Conversation;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.ConversationRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.rtc.dto.MediasoupSignalMessage;
import com.bizcord.rtc.dto.RtcAnswerSignalMessage;
import com.bizcord.rtc.dto.RtcCallEndedMessage;
import com.bizcord.rtc.dto.RtcCallRejectedMessage;
import com.bizcord.rtc.dto.RtcCallRequestMessage;
import com.bizcord.rtc.dto.RtcCallRoomMessage;
import com.bizcord.rtc.dto.RtcIceCandidateSignalMessage;
import com.bizcord.rtc.dto.RtcOfferSignalMessage;
import com.bizcord.rtc.service.MediasoupSidecarService;
import com.bizcord.rtc.service.RtcRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RtcSignalingController {
    private final SimpMessagingTemplate messagingTemplate;
    private final RtcRoomService rtcRoomService;
    private final MediasoupSidecarService mediasoupSidecarService;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    @MessageMapping("/rtc/call-request")
    public void handleCallRequest(
            @Payload RtcCallRequestMessage message,
            Principal principal) {

        log.info("call-request payload: callerId={} roomId={} callType={}",
                message.callerId(), message.roomId(), message.callType());

        User caller = resolveCurrentUser(principal);
        if (caller == null || message.roomId() == null || message.roomId().isBlank()) {
            log.warn("call-request: early exit — caller={} roomId={}",
                    caller != null ? caller.getId() : "NULL", message.roomId());
            return;
        }
        log.info("call-request: caller resolved id={}", caller.getId());

        Conversation conversation = resolveConversation(message.roomId());
        if (conversation == null) {
            log.warn("call-request: conversation not found for roomId={}", message.roomId());
            return;
        }
        log.info("call-request: conversation found id={}", conversation.getId());

        String calleeUserId;
        try {
            calleeUserId = findOtherParticipantUserId(conversation, caller.getId());
        } catch (Exception e) {
            log.error("call-request: findOtherParticipantUserId threw — likely LazyInitializationException", e);
            return;
        }

        if (calleeUserId == null) {
            log.warn("call-request: sender {} is not a participant in room={}", caller.getId(), message.roomId());
            return;
        }
        String calleePrincipalName = toPrincipalName(calleeUserId);
        if (calleePrincipalName == null) {
            return;
        }

        log.debug("call-request from {} to {} (room={})", caller.getId(), calleeUserId, message.roomId());

        rtcRoomService.joinRoom(message.roomId(), caller.getEmail());

        log.info("call-request: sending to callee principalName={}", calleePrincipalName);
        messagingTemplate.convertAndSendToUser(
                calleePrincipalName,
                "/queue/rtc/call-request",
                new RtcCallRequestMessage(
                        caller.getId(),
                        caller.getFullName(),
                        caller.getImageUrl(),
                        message.callType() != null ? message.callType() : "dm",
                        message.roomId()));
        log.info("call-request: sent successfully to callee principalName={}", calleePrincipalName);
    }

    @MessageMapping("/rtc/reject-call")
    public void handleRejectCall(
            @Payload RtcCallRoomMessage message,
            Principal principal) {

        User rejecter = resolveCurrentUser(principal);
        if (rejecter == null || message.roomId() == null || message.roomId().isBlank()) {
            return;
        }

        Conversation conversation = resolveConversation(message.roomId());
        if (conversation == null) {
            return;
        }

        String otherUserId = findOtherParticipantUserId(conversation, rejecter.getId());
        if (otherUserId == null) {
            log.warn("reject-call: sender {} is not a participant in room={}", rejecter.getId(), message.roomId());
            return;
        }
        String otherPrincipalName = toPrincipalName(otherUserId);
        if (otherPrincipalName == null) {
            return;
        }

        log.debug("reject-call from {} to {} (room={})", rejecter.getId(), otherUserId, message.roomId());

        rtcRoomService.leaveRoom(message.roomId(), rejecter.getEmail());
        String otherEmail = toEmail(otherUserId);
        if (otherEmail != null) {
            rtcRoomService.leaveRoom(message.roomId(), otherEmail);
        }

        messagingTemplate.convertAndSendToUser(
                otherPrincipalName,
                "/queue/rtc/call-rejected",
                new RtcCallRejectedMessage(rejecter.getId(), message.roomId()));
    }

    @MessageMapping("/rtc/end-call")
    public void handleEndCall(
            @Payload RtcCallRoomMessage message,
            Principal principal) {

        User endedBy = resolveCurrentUser(principal);
        if (endedBy == null || message.roomId() == null || message.roomId().isBlank()) {
            return;
        }

        Conversation conversation = resolveConversation(message.roomId());
        if (conversation == null) {
            return;
        }

        String otherUserId = findOtherParticipantUserId(conversation, endedBy.getId());
        if (otherUserId == null) {
            log.warn("end-call: sender {} is not a participant in room={}", endedBy.getId(), message.roomId());
            return;
        }
        String otherPrincipalName = toPrincipalName(otherUserId);
        if (otherPrincipalName == null) {
            return;
        }

        log.debug("end-call from {} to {} (room={})", endedBy.getId(), otherUserId, message.roomId());

        rtcRoomService.leaveRoom(message.roomId(), endedBy.getEmail());
        String otherEmail = toEmail(otherUserId);
        if (otherEmail != null) {
            rtcRoomService.leaveRoom(message.roomId(), otherEmail);
        }

        messagingTemplate.convertAndSendToUser(
                otherPrincipalName,
                "/queue/rtc/call-ended",
                new RtcCallEndedMessage(endedBy.getId(), message.roomId()));
    }

    @MessageMapping("/rtc/offer")
    public void handleOffer(
            @Payload RtcOfferSignalMessage message,
            Principal principal) {

        User caller = resolveCurrentUser(principal);
        if (caller == null || message.targetUserId() == null || message.targetUserId().isBlank()) {
            return;
        }
        String targetPrincipalName = toPrincipalName(message.targetUserId());
        if (targetPrincipalName == null) {
            return;
        }

        log.debug("SDP offer from {} to {} (room={})", caller.getId(), message.targetUserId(), message.roomId());

        messagingTemplate.convertAndSendToUser(
                targetPrincipalName,
                "/queue/rtc/offer",
                new RtcOfferSignalMessage(
                        caller.getId(),
                        message.targetUserId(),
                        message.roomId(),
                        message.offer()));
    }

    @MessageMapping("/rtc/answer")
    public void handleAnswer(
            @Payload RtcAnswerSignalMessage message,
            Principal principal) {

        User answerer = resolveCurrentUser(principal);
        if (answerer == null || message.targetUserId() == null || message.targetUserId().isBlank()) {
            return;
        }
        String targetPrincipalName = toPrincipalName(message.targetUserId());
        if (targetPrincipalName == null) {
            return;
        }

        log.debug("SDP answer from {} to {} (room={})", answerer.getId(), message.targetUserId(), message.roomId());

        if (message.roomId() != null) {
            rtcRoomService.joinRoom(message.roomId(), answerer.getEmail());
        }

        messagingTemplate.convertAndSendToUser(
                targetPrincipalName,
                "/queue/rtc/answer",
                new RtcAnswerSignalMessage(
                        answerer.getId(),
                        message.targetUserId(),
                        message.roomId(),
                        message.answer()));
    }

    @MessageMapping("/rtc/ice-candidate")
    public void handleIceCandidate(
            @Payload RtcIceCandidateSignalMessage message,
            Principal principal) {

        User sender = resolveCurrentUser(principal);
        if (sender == null || message.targetUserId() == null || message.targetUserId().isBlank()) {
            return;
        }
        String targetPrincipalName = toPrincipalName(message.targetUserId());
        if (targetPrincipalName == null) {
            return;
        }

        messagingTemplate.convertAndSendToUser(
                targetPrincipalName,
                "/queue/rtc/ice-candidate",
                new RtcIceCandidateSignalMessage(
                        sender.getId(),
                        message.targetUserId(),
                        message.roomId(),
                        message.candidate()));
    }

    private User resolveCurrentUser(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            log.warn("RTC signaling rejected: missing authenticated principal");
            return null;
        }

        return userRepository.findByEmail(principal.getName())
                .orElseGet(() -> {
                    log.warn("RTC signaling rejected: no user found for principal={}", principal.getName());
                    return null;
                });
    }

    private Conversation resolveConversation(String roomId) {
        return conversationRepository.findByIdWithMembers(roomId)
                .orElseGet(() -> {
                    log.warn("RTC signaling rejected: conversation not found for roomId={}", roomId);
                    return null;
                });
    }

    private String findOtherParticipantUserId(Conversation conversation, String senderUserId) {
        String userId1 = conversation.getMember1().getUser().getId();
        String userId2 = conversation.getMember2().getUser().getId();

        if (senderUserId.equals(userId1)) {
            return userId2;
        }
        if (senderUserId.equals(userId2)) {
            return userId1;
        }
        return null;
    }

    private String toPrincipalName(String userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElseGet(() -> {
                    log.warn("RTC signaling dropped: target user not found id={}", userId);
                    return null;
                });
    }

    private String toEmail(String userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse(null);
    }

    private String resolvePrincipalName(Principal principal, String route) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            log.warn("{} rejected: missing authenticated principal", route);
            return null;
        }
        return principal.getName();
    }

    @MessageMapping("/rtc/ms-get-caps")
    public void handleGetRouterCaps(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-get-caps");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-get-caps requested by {} but mediasoup sidecar is unavailable", userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-router-caps",
                    Map.of("error", "mediasoup_unavailable"));
            return;
        }

        String channelId = message.channelId();
        Map<String, Object> caps = mediasoupSidecarService.getRouterCapabilities(channelId);
        if (caps != null) {
            Map<String, Object> response = new HashMap<>(caps);
            response.put("channelId", channelId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-router-caps", response);
        } else {
            log.warn("ms-get-caps: sidecar returned null for channelId={} user={}", channelId, userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-router-caps",
                    Map.of("channelId", channelId, "error", "sidecar_error"));
        }
    }

    @MessageMapping("/rtc/ms-create-transport")
    public void handleCreateTransport(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-create-transport");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-create-transport requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String channelId = message.channelId();
        String direction = message.direction() != null ? message.direction() : "send";
        Map<String, Object> transportParams =
                mediasoupSidecarService.createWebRtcTransport(channelId, direction);

        if (transportParams != null) {
            Map<String, Object> response = new HashMap<>(transportParams);
            response.put("channelId", channelId);
            response.put("direction", direction);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-transport-params", response);
        }
    }

    @MessageMapping("/rtc/ms-connect-transport")
    public void handleConnectTransport(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-connect-transport");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-connect-transport requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String channelId = message.channelId();
        String transportId = message.transportId();
        Map<String, Object> dtlsParameters = message.dtlsParameters() != null ? message.dtlsParameters() : Map.of();

        mediasoupSidecarService.connectTransport(channelId, transportId, dtlsParameters);
        log.debug("ms-connect-transport for channel={} transport={} user={}", channelId, transportId, userId);
    }

    @MessageMapping("/rtc/ms-produce")
    public void handleProduce(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-produce");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-produce requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String channelId = message.channelId();
        String transportId = message.transportId();
        String kind = message.kind() != null ? message.kind() : "audio";
        Map<String, Object> rtpParameters = message.rtpParameters() != null ? message.rtpParameters() : Map.of();

        Map<String, Object> result =
                mediasoupSidecarService.produce(channelId, transportId, kind, rtpParameters, userId);

        if (result != null) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-producer-id", result);

            if (channelId != null) {
                Map<String, Object> notification = new HashMap<>(result);
                notification.put("producerUserId", userId);
                notification.put("kind", kind);
                notification.put("channelId", channelId);

                rtcRoomService.getParticipants(channelId).stream()
                        .filter(p -> !p.equals(userId))
                        .forEach(p -> messagingTemplate.convertAndSendToUser(
                                p, "/queue/rtc/ms-new-producer", notification));
            }
        }
    }

    @MessageMapping("/rtc/ms-consume")
    public void handleConsume(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-consume");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-consume requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String channelId = message.channelId();
        String transportId = message.transportId();
        String producerId = message.producerId();
        Map<String, Object> rtpCapabilities = message.rtpCapabilities() != null ? message.rtpCapabilities() : Map.of();

        Map<String, Object> result =
                mediasoupSidecarService.consume(channelId, transportId, producerId, rtpCapabilities);

        if (result != null) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-new-consumer", result);
        }
    }

    @MessageMapping("/rtc/ms-resume-consumer")
    public void handleResumeConsumer(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-resume-consumer");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-resume-consumer requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String channelId = message.channelId();
        String consumerId = message.consumerId();
        mediasoupSidecarService.resumeConsumer(channelId, consumerId);
        log.debug("ms-resume-consumer for channel={} consumer={} user={}", channelId, consumerId, userId);
    }

    @MessageMapping("/rtc/ms-close-producer")
    public void handleCloseProducer(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-close-producer");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-close-producer requested by {} but mediasoup sidecar is unavailable", userId);
            return;
        }

        String channelId = message.channelId();
        String producerId = message.producerId();
        mediasoupSidecarService.closeProducer(channelId, producerId);
        log.debug("ms-close-producer for channel={} producer={} user={}", channelId, producerId, userId);
    }

    @MessageMapping("/rtc/ms-get-producers")
    public void handleGetProducers(
            @Payload MediasoupSignalMessage message,
            Principal principal) {

        String userId = resolvePrincipalName(principal, "ms-get-producers");
        if (userId == null) {
            return;
        }

        if (!mediasoupSidecarService.isMediasoupAvailable()) {
            log.warn("ms-get-producers requested by {} but mediasoup sidecar is unavailable", userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-producers",
                    Map.of("error", "mediasoup_unavailable"));
            return;
        }

        String channelId = message.channelId();
        List<Map<String, Object>> producers = mediasoupSidecarService.getProducers(channelId);
        if (producers != null) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-producers",
                    Map.of("channelId", channelId, "producers", producers));
        } else {
            log.warn("ms-get-producers: sidecar returned null for channelId={} user={}", channelId, userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/rtc/ms-producers",
                    Map.of("channelId", channelId, "error", "sidecar_error"));
        }
    }
}
