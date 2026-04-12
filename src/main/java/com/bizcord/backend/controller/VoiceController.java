package com.bizcord.backend.controller;

import com.bizcord.backend.service.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {
    private final VoiceService voiceService;

    @GetMapping("/{channelId}/participants")
    public ResponseEntity<Map<String, Set<String>>> getParticipants(@PathVariable String channelId) {
        return ResponseEntity.ok(Map.of("participants", voiceService.getParticipants(channelId)));
    }

    @GetMapping("/server/{serverId}/participants")
    public ResponseEntity<Map<String, List<Map<String, String>>>> getServerParticipants(@PathVariable String serverId) {
        return ResponseEntity.ok(voiceService.getServerVoiceParticipants(serverId));
    }
}
