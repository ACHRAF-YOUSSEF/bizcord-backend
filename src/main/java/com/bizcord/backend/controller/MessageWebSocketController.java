package com.bizcord.backend.controller;

import com.bizcord.backend.dto.MessageCreateRequest;
import com.bizcord.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {
    private final MessageService messageService;

    @MessageMapping("/channels/{channelId}/messages")
    public void sendMessage(
            @DestinationVariable String channelId,
            MessageCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        messageService.createMessage(channelId, request, userDetails.getUsername());
    }
}

