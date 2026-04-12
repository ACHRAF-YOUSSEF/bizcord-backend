package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.DirectMessageCreateRequest;
import com.bizcord.backend.dto.DirectMessageResponse;
import com.bizcord.backend.dto.DirectMessageUpdateRequest;
import com.bizcord.backend.dto.DmSearchResponse;
import com.bizcord.backend.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class DirectMessageController {
    private final DirectMessageService directMessageService;

    @RateLimit(limit = 60, keyType = RateLimitKeyType.UID)
    @GetMapping
    public ResponseEntity<List<DirectMessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                directMessageService.getMessages(conversationId, cursor, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @PostMapping
    public ResponseEntity<DirectMessageResponse> createMessage(
            @PathVariable String conversationId,
            @RequestBody DirectMessageCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(CREATED).body(
                directMessageService.createMessage(conversationId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @PatchMapping("/{messageId}")
    public ResponseEntity<DirectMessageResponse> updateMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestBody DirectMessageUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                directMessageService.updateMessage(conversationId, messageId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{messageId}")
    public ResponseEntity<DirectMessageResponse> deleteMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                directMessageService.deleteMessage(conversationId, messageId, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/search")
    public ResponseEntity<List<DmSearchResponse>> searchMessages(
            @PathVariable String conversationId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                directMessageService.searchMessages(conversationId, q, page, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @PostMapping("/{messageId}/pin")
    public ResponseEntity<DirectMessageResponse> togglePin(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                directMessageService.togglePin(conversationId, messageId, userDetails.getUsername()));
    }

    @RateLimit(limit = 60, keyType = RateLimitKeyType.UID)
    @GetMapping("/pinned")
    public ResponseEntity<List<DirectMessageResponse>> getPinnedMessages(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                directMessageService.getPinnedMessages(conversationId, userDetails.getUsername()));
    }
}

