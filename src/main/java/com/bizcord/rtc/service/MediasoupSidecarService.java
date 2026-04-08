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
        return get("/rooms/" + roomId + "/capabilities");
    }

    public Map<String, Object> createWebRtcTransport(String roomId, String direction) {
        return post("/rooms/" + roomId + "/transports",
                Map.of("direction", direction));
    }

    public void connectTransport(String roomId, String transportId,
                                  Map<String, Object> dtlsParams) {
        post("/rooms/" + roomId + "/transports/" + transportId + "/connect",
                Map.of("dtlsParameters", dtlsParams));
    }

    public Map<String, Object> produce(String roomId, String transportId,
                                        String kind, Map<String, Object> rtpParameters) {
        return post("/rooms/" + roomId + "/transports/" + transportId + "/produce",
                Map.of("kind", kind, "rtpParameters", rtpParameters));
    }

    public Map<String, Object> consume(String roomId, String transportId,
                                        String producerId, Map<String, Object> rtpCapabilities) {
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


