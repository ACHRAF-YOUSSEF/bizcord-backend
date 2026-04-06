package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.MessageCreateRequest;
import com.bizcord.backend.dto.MessageResponse;
import com.bizcord.backend.dto.MessageUpdateRequest;
import com.bizcord.backend.service.MessageService;
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
@RequestMapping("/api/channels/{channelId}/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @RateLimit(limit = 60, keyType = RateLimitKeyType.UID)
    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String channelId,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                messageService.getMessages(channelId, cursor, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable String channelId,
            @RequestBody MessageCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(CREATED).body(
                messageService.createMessage(channelId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageResponse> updateMessage(
            @PathVariable String channelId,
            @PathVariable String messageId,
            @RequestBody MessageUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                messageService.updateMessage(channelId, messageId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{messageId}")
    public ResponseEntity<MessageResponse> deleteMessage(
            @PathVariable String channelId,
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                messageService.deleteMessage(channelId, messageId, userDetails.getUsername()));
    }
}

