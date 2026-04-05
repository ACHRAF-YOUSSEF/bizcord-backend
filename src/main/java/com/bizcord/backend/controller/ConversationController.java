package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.ConversationResponse;
import com.bizcord.backend.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(conversationService.getConversations(userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(conversationService.getConversationById(conversationId, userDetails.getUsername()));
    }

    @RateLimit(limit = 20, keyType = RateLimitKeyType.UID)
    @PostMapping
    public ResponseEntity<ConversationResponse> getOrCreateConversation(
            @RequestParam String memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ConversationResponse response = conversationService.getOrCreateConversation(memberId, userDetails.getUsername());
        return ResponseEntity.status(CREATED).body(response);
    }
}

