package com.bizcord.backend.controller;

import com.bizcord.backend.dto.TypingEvent;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class TypingWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/channels/{channelId}/typing")
    public void channelTyping(
            @DestinationVariable String channelId,
            @Payload TypingEvent request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        TypingEvent event = TypingEvent.builder()
                .type(request.getType())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .imageUrl(user.getImageUrl())
                .build();
        messagingTemplate.convertAndSend("/topic/channels/" + channelId + "/typing", event);
    }

    @MessageMapping("/conversations/{conversationId}/typing")
    public void conversationTyping(
            @DestinationVariable String conversationId,
            @Payload TypingEvent request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        TypingEvent event = TypingEvent.builder()
                .type(request.getType())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .imageUrl(user.getImageUrl())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing", event);
    }
}
