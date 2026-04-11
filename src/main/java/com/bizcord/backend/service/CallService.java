package com.bizcord.backend.service;

import com.bizcord.backend.dto.WebSocketMessage;
import com.bizcord.backend.entity.Conversation;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.ConversationRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallService {
    private final MediasoupClient mediasoupClient;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // conversationId -> Set<userId> for tracking who's in each call
    private final ConcurrentHashMap<String, Set<String>> callParticipants = new ConcurrentHashMap<>();
    // sessionId -> [email, conversationId] for disconnect cleanup
    private final ConcurrentHashMap<String, String[]> sessionMap = new ConcurrentHashMap<>();
    // conversationId -> Instant when the room became lonely (0-1 participants)
    private final ConcurrentHashMap<String, Instant> roomLonelyTimestamps = new ConcurrentHashMap<>();

    /**
     * Check if there's an active call (mediasoup room with participants) for a conversation.
     */
    public boolean hasActiveCall(String conversationId) {
        Set<String> participants = callParticipants.get(conversationId);
        return participants != null && !participants.isEmpty();
    }

    /**
     * Initiate a call — sends CALL_INCOMING to the other participant.
     */
    public void initiateCall(String conversationId, boolean withVideo, String email) {
        User caller = getUser(email);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, caller);

        User otherUser = getOtherUser(conversation, caller);

        // Notify the recipient via their user queue
        messagingTemplate.convertAndSendToUser(
                otherUser.getEmail(),
                "/queue/calls",
                new WebSocketMessage("CALL_INCOMING", Map.of(
                        "conversationId", conversationId,
                        "callerId", caller.getId(),
                        "callerName", caller.getUsername2(),
                        "callerImage", caller.getImageUrl() != null ? caller.getImageUrl() : "",
                        "withVideo", withVideo
                ))
        );
    }

    /**
     * Accept a call — both parties can now join the mediasoup room.
     */
    public void acceptCall(String conversationId, String email) {
        User user = getUser(email);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, user);

        User otherUser = getOtherUser(conversation, user);

        // Notify the caller that the call was accepted
        messagingTemplate.convertAndSendToUser(
                otherUser.getEmail(),
                "/queue/calls",
                new WebSocketMessage("CALL_ACCEPTED", Map.of(
                        "conversationId", conversationId,
                        "userId", user.getId(),
                        "username", user.getUsername2()
                ))
        );
    }

    /**
     * Decline a call.
     */
    public void declineCall(String conversationId, String email) {
        User user = getUser(email);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, user);

        User otherUser = getOtherUser(conversation, user);

        messagingTemplate.convertAndSendToUser(
                otherUser.getEmail(),
                "/queue/calls",
                new WebSocketMessage("CALL_DECLINED", Map.of(
                        "conversationId", conversationId,
                        "userId", user.getId()
                ))
        );
    }

    /**
     * End a call — notifies the other party and cleans up.
     */
    public void endCall(String conversationId, String email) {
        User user = getUser(email);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, user);

        // Broadcast to call topic
        broadcast(conversationId, "CALL_ENDED", Map.of(
                "conversationId", conversationId,
                "userId", user.getId()
        ));

        // Clean up this user's participation
        leave(conversationId, email);
    }

    // ── Mediasoup room methods (reuse pattern from VoiceService, no server broadcasts) ──

    @SuppressWarnings("unchecked")
    public Map<String, Object> join(String conversationId, String email, String sessionId) {
        User user = getUser(email);
        Conversation conversation = getConversation(conversationId);
        assertParticipant(conversation, user);

        // Clean up any previous call session for this user
        sessionMap.values().removeIf(v -> v[0].equals(email));
        for (var entry : callParticipants.entrySet()) {
            entry.getValue().remove(user.getId());
        }

        Map<String, Object> result = new java.util.HashMap<>(mediasoupClient.joinRoom(conversationId, user.getId()));

        // Enrich peers with username info
        List<Map<String, Object>> peers = (List<Map<String, Object>>) result.get("peers");
        if (peers != null) {
            for (Map<String, Object> peer : peers) {
                String peerId = (String) peer.get("peerId");
                userRepository.findById(peerId).ifPresent(u ->
                        peer.put("username", u.getUsername2())
                );
            }
        }

        callParticipants.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(user.getId());
        updateLonelyTimestamp(conversationId);
        if (sessionId != null) {
            sessionMap.put(sessionId, new String[]{email, conversationId});
        }

        broadcast(conversationId, "CALL_JOIN", Map.of(
                "userId", user.getId(),
                "username", user.getUsername2(),
                "conversationId", conversationId
        ));

        return result;
    }

    public void leave(String conversationId, String email) {
        User user = getUser(email);

        try {
            mediasoupClient.leaveRoom(conversationId, user.getId());
        } catch (Exception e) {
            log.warn("Error leaving mediasoup room: {}", e.getMessage());
        }

        sessionMap.values().removeIf(v -> v[0].equals(email) && v[1].equals(conversationId));

        Set<String> participants = callParticipants.get(conversationId);
        if (participants != null) {
            participants.remove(user.getId());
            if (participants.isEmpty()) {
                callParticipants.remove(conversationId);
                roomLonelyTimestamps.remove(conversationId);
            } else {
                updateLonelyTimestamp(conversationId);
            }
        }

        broadcast(conversationId, "CALL_LEAVE", Map.of(
                "userId", user.getId(),
                "conversationId", conversationId
        ));
    }

    public Map<String, Object> createTransport(String conversationId, String email) {
        User user = getUser(email);
        return mediasoupClient.createTransport(conversationId, user.getId());
    }

    public void connectTransport(String conversationId, String transportId, Object dtlsParameters, String email) {
        User user = getUser(email);
        mediasoupClient.connectTransport(conversationId, user.getId(), transportId, dtlsParameters);
    }

    public Map<String, Object> produce(String conversationId, String transportId, String kind,
                                        Object rtpParameters, Map<String, Object> appData, String email) {
        User user = getUser(email);
        Map<String, Object> result = mediasoupClient.produce(conversationId, user.getId(), transportId, kind, rtpParameters, appData);

        broadcast(conversationId, "NEW_PRODUCER", Map.of(
                "userId", user.getId(),
                "producerId", result.get("id"),
                "kind", kind,
                "appData", appData != null ? appData : Map.of()
        ));

        return result;
    }

    public Map<String, Object> consume(String conversationId, String transportId, String producerId,
                                        Object rtpCapabilities, String email) {
        User user = getUser(email);
        return mediasoupClient.consume(conversationId, user.getId(), transportId, producerId, rtpCapabilities);
    }

    public void resumeConsumer(String conversationId, String consumerId, String email) {
        User user = getUser(email);
        mediasoupClient.resumeConsumer(conversationId, user.getId(), consumerId);
    }

    public void pauseProducer(String conversationId, String producerId, String email) {
        User user = getUser(email);
        mediasoupClient.pauseProducer(conversationId, user.getId(), producerId);
        broadcast(conversationId, "PRODUCER_PAUSED", Map.of("userId", user.getId(), "producerId", producerId));
    }

    public void resumeProducer(String conversationId, String producerId, String email) {
        User user = getUser(email);
        mediasoupClient.resumeProducer(conversationId, user.getId(), producerId);
        broadcast(conversationId, "PRODUCER_RESUMED", Map.of("userId", user.getId(), "producerId", producerId));
    }

    public void closeProducer(String conversationId, String producerId, String email) {
        User user = getUser(email);
        mediasoupClient.closeProducer(conversationId, user.getId(), producerId);
        broadcast(conversationId, "PRODUCER_CLOSED", Map.of("userId", user.getId(), "producerId", producerId));
    }

    // ── Helpers ──

    private void broadcast(String conversationId, String type, Object data) {
        messagingTemplate.convertAndSend("/topic/calls/" + conversationId, new WebSocketMessage(type, data));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private Conversation getConversation(String conversationId) {
        return conversationRepository.findByIdWithUsers(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND));
    }

    private void assertParticipant(Conversation conversation, User user) {
        if (!conversation.getUser1().getId().equals(user.getId()) &&
                !conversation.getUser2().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant of this conversation");
        }
    }

    private User getOtherUser(Conversation conversation, User currentUser) {
        return conversation.getUser1().getId().equals(currentUser.getId())
                ? conversation.getUser2()
                : conversation.getUser1();
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String[] info = sessionMap.remove(sessionId);
        if (info == null) return;

        String email = info[0];
        String conversationId = info[1];

        try {
            User user = getUser(email);
            mediasoupClient.leaveRoom(conversationId, user.getId());

            Set<String> participants = callParticipants.get(conversationId);
            if (participants != null) {
                participants.remove(user.getId());
                if (participants.isEmpty()) {
                    callParticipants.remove(conversationId);
                    roomLonelyTimestamps.remove(conversationId);
                } else {
                    updateLonelyTimestamp(conversationId);
                }
            }

            broadcast(conversationId, "CALL_LEAVE", Map.of(
                    "userId", user.getId(),
                    "conversationId", conversationId
            ));

            log.info("Cleaned up call session for user {} in conversation {}", email, conversationId);
        } catch (Exception e) {
            log.warn("Error cleaning up call session: {}", e.getMessage());
        }
    }

    /**
     * Update the lonely timestamp for a room based on current participant count.
     * If 0-1 participants, start the lonely timer; if 2+, clear it.
     */
    private void updateLonelyTimestamp(String conversationId) {
        Set<String> participants = callParticipants.get(conversationId);
        int count = participants != null ? participants.size() : 0;
        if (count <= 1) {
            roomLonelyTimestamps.putIfAbsent(conversationId, Instant.now());
        } else {
            roomLonelyTimestamps.remove(conversationId);
        }
    }

    /**
     * Every 60 seconds, check for rooms that have been lonely (0-1 participants) for 10+ minutes
     * and clean them up automatically.
     */
    @Scheduled(fixedDelay = 60_000)
    public void expireLonelyRooms() {
        Instant cutoff = Instant.now().minusSeconds(600); // 10 minutes

        for (var entry : roomLonelyTimestamps.entrySet()) {
            String conversationId = entry.getKey();
            Instant lonelySince = entry.getValue();

            if (lonelySince.isBefore(cutoff)) {
                log.info("Auto-expiring lonely call room for conversation {}", conversationId);

                // Clean up mediasoup room for remaining participants
                Set<String> remaining = callParticipants.remove(conversationId);
                roomLonelyTimestamps.remove(conversationId);

                if (remaining != null) {
                    for (String userId : remaining) {
                        try {
                            mediasoupClient.leaveRoom(conversationId, userId);
                        } catch (Exception e) {
                            log.warn("Error removing user {} from expired room: {}", userId, e.getMessage());
                        }
                    }
                }

                // Remove any session mappings for this room
                sessionMap.values().removeIf(v -> v[1].equals(conversationId));

                // Broadcast CALL_ENDED to all subscribers
                broadcast(conversationId, "CALL_ENDED", Map.of(
                        "conversationId", conversationId,
                        "reason", "expired"
                ));
            }
        }
    }
}
