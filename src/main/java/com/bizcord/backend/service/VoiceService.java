package com.bizcord.backend.service;

import com.bizcord.backend.dto.WebSocketMessage;
import com.bizcord.backend.entity.Channel;
import com.bizcord.backend.entity.ChannelType;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.ChannelRepository;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {
    private final MediasoupClient mediasoupClient;
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // channelId -> Set<userId> for tracking who's in each voice channel
    private final ConcurrentHashMap<String, Set<String>> voiceParticipants = new ConcurrentHashMap<>();
    // channelId -> serverId for reverse lookup
    private final ConcurrentHashMap<String, String> channelServerMap = new ConcurrentHashMap<>();
    // sessionId -> [email, channelId] for disconnect cleanup
    private final ConcurrentHashMap<String, String[]> sessionMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public Map<String, Object> join(String channelId, String email, String sessionId) {
        User user = getUser(email);
        Channel channel = getVoiceChannel(channelId);
        assertMember(channel.getServer().getId(), user.getId());

        // Clean up any previous voice session for this user
        sessionMap.values().removeIf(v -> v[0].equals(email));
        for (var entry : voiceParticipants.entrySet()) {
            entry.getValue().remove(user.getId());
        }

        Map<String, Object> result = new java.util.HashMap<>(mediasoupClient.joinRoom(channelId, user.getId()));

        // Enrich peers with username info
        List<Map<String, Object>> peers = (List<Map<String, Object>>) result.get("peers");
        if (peers != null) {
            for (Map<String, Object> peer : peers) {
                String peerId = (String) peer.get("peerId");
                userRepository.findById(peerId).ifPresent(u -> {
                        peer.put("username", u.getUsername2());
                        peer.put("imageUrl", u.getImageUrl());
                });
            }
        }

        voiceParticipants.computeIfAbsent(channelId, k -> ConcurrentHashMap.newKeySet()).add(user.getId());
        channelServerMap.put(channelId, channel.getServer().getId());
        if (sessionId != null) {
            sessionMap.put(sessionId, new String[]{email, channelId});
        }

        var joinPayload = new java.util.HashMap<String, Object>();
        joinPayload.put("userId", user.getId());
        joinPayload.put("username", user.getUsername2());
        joinPayload.put("channelId", channelId);
        joinPayload.put("imageUrl", user.getImageUrl());
        broadcast(channelId, "VOICE_JOIN", joinPayload);
        broadcastServer(channel.getServer().getId(), "VOICE_JOIN", joinPayload);

        return result;
    }

    public void leave(String channelId, String email) {
        User user = getUser(email);

        mediasoupClient.leaveRoom(channelId, user.getId());

        // Clean up session tracking
        sessionMap.values().removeIf(v -> v[0].equals(email) && v[1].equals(channelId));

        Set<String> participants = voiceParticipants.get(channelId);
        if (participants != null) {
            participants.remove(user.getId());
            if (participants.isEmpty()) {
                voiceParticipants.remove(channelId);
                channelServerMap.remove(channelId);
            }
        }

        var leavePayload = Map.of(
                "userId", user.getId(),
                "channelId", channelId
        );
        broadcast(channelId, "VOICE_LEAVE", leavePayload);

        String serverId = channelServerMap.get(channelId);
        if (serverId == null) {
            // Channel might have been cleaned up, look it up
            channelRepository.findById(channelId).ifPresent(ch ->
                    broadcastServer(ch.getServer().getId(), "VOICE_LEAVE", leavePayload)
            );
        } else {
            broadcastServer(serverId, "VOICE_LEAVE", leavePayload);
        }
    }

    public Map<String, Object> createTransport(String channelId, String email) {
        User user = getUser(email);
        return mediasoupClient.createTransport(channelId, user.getId());
    }

    public void connectTransport(String channelId, String transportId, Object dtlsParameters, String email) {
        User user = getUser(email);
        mediasoupClient.connectTransport(channelId, user.getId(), transportId, dtlsParameters);
    }

    public Map<String, Object> produce(String channelId, String transportId, String kind,
                                         Object rtpParameters, Map<String, Object> appData, String email) {
        User user = getUser(email);
        Map<String, Object> result = mediasoupClient.produce(channelId, user.getId(), transportId, kind, rtpParameters, appData);

        broadcast(channelId, "NEW_PRODUCER", Map.of(
                "userId", user.getId(),
                "producerId", result.get("id"),
                "kind", kind,
                "appData", appData != null ? appData : Map.of()
        ));

        return result;
    }

    public Map<String, Object> consume(String channelId, String transportId, String producerId,
                                        Object rtpCapabilities, String email) {
        User user = getUser(email);
        return mediasoupClient.consume(channelId, user.getId(), transportId, producerId, rtpCapabilities);
    }

    public void resumeConsumer(String channelId, String consumerId, String email) {
        User user = getUser(email);
        mediasoupClient.resumeConsumer(channelId, user.getId(), consumerId);
    }

    public void pauseProducer(String channelId, String producerId, String email) {
        User user = getUser(email);
        mediasoupClient.pauseProducer(channelId, user.getId(), producerId);

        broadcast(channelId, "PRODUCER_PAUSED", Map.of(
                "userId", user.getId(),
                "producerId", producerId
        ));
    }

    public void resumeProducer(String channelId, String producerId, String email) {
        User user = getUser(email);
        mediasoupClient.resumeProducer(channelId, user.getId(), producerId);

        broadcast(channelId, "PRODUCER_RESUMED", Map.of(
                "userId", user.getId(),
                "producerId", producerId
        ));
    }

    public void closeProducer(String channelId, String producerId, String email) {
        User user = getUser(email);
        mediasoupClient.closeProducer(channelId, user.getId(), producerId);

        broadcast(channelId, "PRODUCER_CLOSED", Map.of(
                "userId", user.getId(),
                "producerId", producerId
        ));
    }

    public Set<String> getParticipants(String channelId) {
        return voiceParticipants.getOrDefault(channelId, Set.of());
    }

    public Map<String, List<Map<String, String>>> getServerVoiceParticipants(String serverId) {
        Map<String, List<Map<String, String>>> result = new java.util.HashMap<>();
        for (var entry : channelServerMap.entrySet()) {
            if (serverId.equals(entry.getValue())) {
                String chId = entry.getKey();
                Set<String> userIds = voiceParticipants.get(chId);
                if (userIds != null && !userIds.isEmpty()) {
                    List<Map<String, String>> list = new java.util.ArrayList<>();
                    for (String uid : userIds) {
                        userRepository.findById(uid).ifPresent(u -> {
                                var participant = new java.util.HashMap<String, String>();
                                participant.put("userId", u.getId());
                                participant.put("username", u.getUsername2());
                                participant.put("imageUrl", u.getImageUrl());
                                list.add(participant);
                        });
                    }
                    if (!list.isEmpty()) {
                        result.put(chId, list);
                    }
                }
            }
        }
        return result;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private Channel getVoiceChannel(String channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CHANNEL_NOT_FOUND));
        if (channel.getType() != ChannelType.VOICE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel is not a voice channel");
        }
        return channel;
    }

    private void assertMember(String serverId, String userId) {
        if (!memberRepository.existsByServerIdAndUserId(serverId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.NOT_A_MEMBER);
        }
    }

    private void broadcast(String channelId, String type, Object data) {
        messagingTemplate.convertAndSend("/topic/voice/" + channelId, new WebSocketMessage(type, data));
    }

    private void broadcastServer(String serverId, String type, Object data) {
        messagingTemplate.convertAndSend("/topic/server/" + serverId + "/voice", new WebSocketMessage(type, data));
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String[] info = sessionMap.remove(sessionId);
        if (info == null) return;

        String email = info[0];
        String channelId = info[1];

        try {
            User user = getUser(email);
            mediasoupClient.leaveRoom(channelId, user.getId());

            Set<String> participants = voiceParticipants.get(channelId);
            if (participants != null) {
                participants.remove(user.getId());
                if (participants.isEmpty()) {
                    voiceParticipants.remove(channelId);
                    channelServerMap.remove(channelId);
                }
            }

            var leavePayload = Map.of(
                    "userId", user.getId(),
                    "channelId", channelId
            );
            broadcast(channelId, "VOICE_LEAVE", leavePayload);

            String serverId = channelServerMap.get(channelId);
            if (serverId == null) {
                channelRepository.findById(channelId).ifPresent(ch ->
                        broadcastServer(ch.getServer().getId(), "VOICE_LEAVE", leavePayload)
                );
            } else {
                broadcastServer(serverId, "VOICE_LEAVE", leavePayload);
            }

            log.info("Cleaned up voice session for user {} in channel {}", email, channelId);
        } catch (Exception e) {
            log.warn("Error cleaning up voice session: {}", e.getMessage());
        }
    }
}
