package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.MessageSearchResponse;
import com.bizcord.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/messages/search")
@RequiredArgsConstructor
public class MessageSearchController {
    private final MessageService messageService;

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping
    public ResponseEntity<List<MessageSearchResponse>> search(
            @PathVariable String serverId,
            @RequestParam String q,
            @RequestParam(required = false) String channelId,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                messageService.searchMessages(serverId, channelId, q, page, userDetails.getUsername()));
    }
}
