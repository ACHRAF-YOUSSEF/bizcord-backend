package com.bizcord.rtc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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
                                  Map<String, Object> dtlsParams) {
        if (roomId == null || roomId.isBlank() || transportId == null || transportId.isBlank()) {
            log.warn("connectTransport called with null/blank roomId or transportId — skipping");
            return;
        }
        post("/rooms/" + roomId + "/transports/" + transportId + "/connect",
                Map.of("dtlsParameters", dtlsParams));
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

    public Map<String, Object> consume(String roomId, String transportId,
                                        String producerId, Map<String, Object> rtpCapabilities) {
        if (roomId == null || roomId.isBlank() || transportId == null || transportId.isBlank()) {
            log.warn("consume called with null/blank roomId or transportId — skipping");
            return null;
        }
        return post("/rooms/" + roomId + "/transports/" + transportId + "/consume",
                Map.of("producerId", producerId, "rtpCapabilities", rtpCapabilities));
    }

    public boolean isMediasoupAvailable() {
        return mediasoupAvailable;
    }


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

    private void handleSidecarDown(String path, ResourceAccessException ex) {
        if (mediasoupAvailable) {
            log.warn("Mediasoup sidecar is unreachable at {} — SFU features disabled. "
                    + "1-on-1 calls continue to work. Cause: {}", path, ex.getMessage());
        }
        mediasoupAvailable = false;
    }
}


