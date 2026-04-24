package com.bizcord.backend.config;

import com.bizcord.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {
    private final UserService userService;

    private static final Pattern SERVER_PRESENCE_PATTERN =
            Pattern.compile("^/topic/servers/([^/]+)/presence$");

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;
        userService.setStatusOnConnect(principal.getName());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) return;
        userService.setStatusOnDisconnect(principal.getName());
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) return;
        Matcher matcher = SERVER_PRESENCE_PATTERN.matcher(destination);
        if (matcher.matches()) {
            userService.broadcastServerPresence(matcher.group(1));
        }
    }
}
