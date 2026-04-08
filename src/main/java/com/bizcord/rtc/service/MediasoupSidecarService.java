package com.bizcord.rtc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MediasoupSidecarService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    private volatile boolean mediasoupAvailable = true;

    public MediasoupSidecarService(
            RestTemplateBuilder builder,
            @Value("${rtc.mediasoup.url}") String baseUrl) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> getRouterCapabilities(String roomId) {
        String path = (roomId != null && !roomId.isBlank())
                ? "/rooms/" + roomId + "/capabilities"
                : "/router/capabilities";
        return get(path);
    }

    public Map<String, Object> createWebRtcTransport(String roomId, String direction) {
        if (roomId == null || roomId.isBlank()) {
            log.warn("createWebRtcTransport called with null/blank roomId — skipping");
            return null;
        }
        return post("/rooms/" + roomId + "/transports",
                Map.of("direction", direction));
    }

    public void connectTransport(String roomId, String transportId,
                                  Map<String, Object> dtlsParameters) {
        if (roomId == null || roomId.isBlank() || transportId == null || transportId.isBlank()) {
            log.warn("connectTransport called with null/blank roomId or transportId — skipping");
            return;
        }
        post("/rooms/" + roomId + "/transports/" + transportId + "/connect",
                Map.of("dtlsParameters", dtlsParameters));
    }

    public void closeTransport(String roomId, String transportId) {
        if (roomId == null || roomId.isBlank() || transportId == null || transportId.isBlank()) {
            log.warn("closeTransport called with null/blank roomId or transportId — skipping");
            return;
        }
        delete("/rooms/" + roomId + "/transports/" + transportId);
    }

    public Map<String, Object> produce(String roomId, String transportId,
                                        String kind, Map<String, Object> rtpParameters) {
        if (roomId == null || roomId.isBlank() || transportId == null || transportId.isBlank()) {
            log.warn("produce called with null/blank roomId or transportId — skipping");
            return null;
        }
        return post("/rooms/" + roomId + "/transports/" + transportId + "/produce",
                Map.of("kind", kind, "rtpParameters", rtpParameters));
    }

    /** GET /rooms/{roomId}/producers */
    public List<Map<String, Object>> getProducers(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            log.warn("getProducers called with null/blank roomId — skipping");
            return null;
        }
        return getList("/rooms/" + roomId + "/producers");
    }

    /** DELETE /rooms/{roomId}/producers/{producerId} */
    public void closeProducer(String roomId, String producerId) {
        if (roomId == null || roomId.isBlank() || producerId == null || producerId.isBlank()) {
            log.warn("closeProducer called with null/blank roomId or producerId — skipping");
            return;
        }
        delete("/rooms/" + roomId + "/producers/" + producerId);
    }

    // ── Consumers ────────────────────────────────────────────────────────────

    /** POST /rooms/{roomId}/transports/{transportId}/consume  body: { producerId, rtpCapabilities } */
    public Map<String, Object> consume(String roomId, String transportId,
                                        String producerId, Map<String, Object> rtpCapabilities) {
        if (roomId == null || roomId.isBlank() || transportId == null || transportId.isBlank()) {
            log.warn("consume called with null/blank roomId or transportId — skipping");
            return null;
        }
        return post("/rooms/" + roomId + "/transports/" + transportId + "/consume",
                Map.of("producerId", producerId, "rtpCapabilities", rtpCapabilities));
    }

    /** POST /rooms/{roomId}/consumers/{consumerId}/resume */
    public void resumeConsumer(String roomId, String consumerId) {
        if (roomId == null || roomId.isBlank() || consumerId == null || consumerId.isBlank()) {
            log.warn("resumeConsumer called with null/blank roomId or consumerId — skipping");
            return;
        }
        post("/rooms/" + roomId + "/consumers/" + consumerId + "/resume", Map.of());
    }

    // ── Health ───────────────────────────────────────────────────────────────

    /** GET /health — actively probes the sidecar and updates the availability flag. */
    public boolean probeHealth() {
        try {
            restTemplate.getForEntity(baseUrl + "/health", Void.class);
            mediasoupAvailable = true;
            return true;
        } catch (Exception ex) {
            if (mediasoupAvailable) {
                log.warn("Mediasoup sidecar health-probe failed — SFU features disabled. Cause: {}",
                        ex.getMessage());
            }
            mediasoupAvailable = false;
            return false;
        }
    }

    public boolean isMediasoupAvailable() {
        return mediasoupAvailable;
    }

    // ── Private HTTP helpers ─────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> get(String path) {
        try {
            ResponseEntity<Map<String, Object>> response =
                    (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>)
                            restTemplate.getForEntity(baseUrl + path, Map.class);
            mediasoupAvailable = true;
            return response.getBody();
        } catch (ResourceAccessException ex) {
            handleSidecarDown(path, ex);
            return null;
        } catch (Exception ex) {
            log.error("Mediasoup sidecar GET {} failed: {}", path, ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> getList(String path) {
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(baseUrl + path, List.class);
            mediasoupAvailable = true;
            return response.getBody();
        } catch (ResourceAccessException ex) {
            handleSidecarDown(path, ex);
            return null;
        } catch (Exception ex) {
            log.error("Mediasoup sidecar GET {} failed: {}", path, ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            ResponseEntity<Map<String, Object>> response =
                    (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>)
                            restTemplate.postForEntity(baseUrl + path, body, Map.class);
            mediasoupAvailable = true;
            return response.getBody();
        } catch (ResourceAccessException ex) {
            handleSidecarDown(path, ex);
            return null;
        } catch (Exception ex) {
            log.error("Mediasoup sidecar POST {} failed: {}", path, ex.getMessage());
            return null;
        }
    }

    private void delete(String path) {
        try {
            restTemplate.exchange(baseUrl + path, HttpMethod.DELETE, null, Void.class);
            mediasoupAvailable = true;
        } catch (ResourceAccessException ex) {
            handleSidecarDown(path, ex);
        } catch (Exception ex) {
            log.error("Mediasoup sidecar DELETE {} failed: {}", path, ex.getMessage());
        }
    }

    private void handleSidecarDown(String path, ResourceAccessException ex) {
        if (mediasoupAvailable) {
            log.warn("Mediasoup sidecar is unreachable at {} — SFU features disabled. "
                    + "1-on-1 calls continue to work. Cause: {}", path, ex.getMessage());
        }
        mediasoupAvailable = false;
    }
}

