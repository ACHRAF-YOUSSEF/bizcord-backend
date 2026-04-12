package com.bizcord.backend.service;

import com.bizcord.backend.config.MediasoupProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class MediasoupClient {
    private final RestClient restClient;

    public MediasoupClient(MediasoupProperties props) {
        this.restClient = RestClient.builder()
                .baseUrl(props.getUrl())
                .defaultHeader("X-Api-Secret", props.getApiSecret())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> joinRoom(String roomId, String peerId) {
        return restClient.post()
                .uri("/api/rooms/{roomId}/join", roomId)
                .body(Map.of("peerId", peerId))
                .retrieve()
                .body(Map.class);
    }

    public void leaveRoom(String roomId, String peerId) {
        restClient.post()
                .uri("/api/rooms/{roomId}/leave", roomId)
                .body(Map.of("peerId", peerId))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createTransport(String roomId, String peerId) {
        return restClient.post()
                .uri("/api/rooms/{roomId}/transports", roomId)
                .body(Map.of("peerId", peerId))
                .retrieve()
                .body(Map.class);
    }

    public void connectTransport(String roomId, String peerId, String transportId, Object dtlsParameters) {
        restClient.post()
                .uri("/api/rooms/{roomId}/transports/{transportId}/connect", roomId, transportId)
                .body(Map.of("peerId", peerId, "dtlsParameters", dtlsParameters))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> produce(String roomId, String peerId, String transportId,
                                        String kind, Object rtpParameters, Map<String, Object> appData) {
        return restClient.post()
                .uri("/api/rooms/{roomId}/produce", roomId)
                .body(Map.of(
                        "peerId", peerId,
                        "transportId", transportId,
                        "kind", kind,
                        "rtpParameters", rtpParameters,
                        "appData", appData != null ? appData : Map.of()
                ))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> consume(String roomId, String peerId, String transportId,
                                        String producerId, Object rtpCapabilities) {
        return restClient.post()
                .uri("/api/rooms/{roomId}/consume", roomId)
                .body(Map.of(
                        "peerId", peerId,
                        "transportId", transportId,
                        "producerId", producerId,
                        "rtpCapabilities", rtpCapabilities
                ))
                .retrieve()
                .body(Map.class);
    }

    public void resumeConsumer(String roomId, String peerId, String consumerId) {
        restClient.post()
                .uri("/api/rooms/{roomId}/consumers/{consumerId}/resume", roomId, consumerId)
                .body(Map.of("peerId", peerId))
                .retrieve()
                .toBodilessEntity();
    }

    public void pauseProducer(String roomId, String peerId, String producerId) {
        restClient.post()
                .uri("/api/rooms/{roomId}/producers/{producerId}/pause", roomId, producerId)
                .body(Map.of("peerId", peerId))
                .retrieve()
                .toBodilessEntity();
    }

    public void resumeProducer(String roomId, String peerId, String producerId) {
        restClient.post()
                .uri("/api/rooms/{roomId}/producers/{producerId}/resume", roomId, producerId)
                .body(Map.of("peerId", peerId))
                .retrieve()
                .toBodilessEntity();
    }

    public void closeProducer(String roomId, String peerId, String producerId) {
        restClient.post()
                .uri("/api/rooms/{roomId}/producers/{producerId}/close", roomId, producerId)
                .body(Map.of("peerId", peerId))
                .retrieve()
                .toBodilessEntity();
    }
}
