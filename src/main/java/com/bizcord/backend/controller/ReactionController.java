package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.ReactionRequest;
import com.bizcord.backend.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;

    @RateLimit(limit = 60, keyType = RateLimitKeyType.UID)
    @PostMapping("/api/channels/{channelId}/messages/{messageId}/reactions")
    public ResponseEntity<Void> toggleChannelMessageReaction(
            @PathVariable String channelId,
            @PathVariable String messageId,
            @RequestBody ReactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        reactionService.toggleChannelMessageReaction(channelId, messageId, request.getEmoji(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @RateLimit(limit = 60, keyType = RateLimitKeyType.UID)
    @PostMapping("/api/conversations/{conversationId}/messages/{messageId}/reactions")
    public ResponseEntity<Void> toggleDirectMessageReaction(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestBody ReactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        reactionService.toggleDirectMessageReaction(conversationId, messageId, request.getEmoji(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
