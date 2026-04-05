package com.bizcord.backend.controller;

import com.bizcord.backend.dto.DirectMessageCreateRequest;
import com.bizcord.backend.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectMessageWebSocketController {
    private final DirectMessageService directMessageService;

    @MessageMapping("/conversations/{conversationId}/messages")
    public void sendMessage(
            @DestinationVariable String conversationId,
            DirectMessageCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        directMessageService.createMessage(conversationId, request, userDetails.getUsername());
    }
}

