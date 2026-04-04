package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.ChannelCreateRequest;
import com.bizcord.backend.dto.ChannelUpdateRequest;
import com.bizcord.backend.dto.ServerResponse;
import com.bizcord.backend.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PostMapping
    public ResponseEntity<ServerResponse> createChannel(@RequestParam String serverId,
                                                        @Valid @RequestBody ChannelCreateRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(CREATED)
                .body(channelService.createChannel(serverId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PatchMapping("/{channelId}")
    public ResponseEntity<ServerResponse> updateChannel(@PathVariable String channelId,
                                                        @RequestParam String serverId,
                                                        @Valid @RequestBody ChannelUpdateRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(channelService.updateChannel(channelId, serverId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{channelId}")
    public ResponseEntity<ServerResponse> deleteChannel(@PathVariable String channelId,
                                                        @RequestParam String serverId,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(channelService.deleteChannel(channelId, serverId, userDetails.getUsername()));
    }
}
