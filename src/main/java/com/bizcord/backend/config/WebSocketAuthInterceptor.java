package com.bizcord.backend.config;

import com.bizcord.backend.config.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !shouldAuthenticate(accessor)) {
            return message;
        }

        String jwt = resolveBearerToken(accessor);
        if (jwt == null || jwt.isBlank()) {
            return message;
        }

        try {
            String userEmail = jwtService.extractUsername(jwt);
            if (userEmail == null || userEmail.isBlank()) {
                return message;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (!jwtService.isTokenValid(jwt, userDetails)) {
                return message;
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);
            accessor.setUser(authToken);
        } catch (Exception ignored) {
            // Invalid/expired/malformed tokens must not crash channel handling.
            return message;
        }

        return message;
    }

    private boolean shouldAuthenticate(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return false;
        }

        StompCommand command = accessor.getCommand();
        return StompCommand.CONNECT.equals(command)
                || StompCommand.SEND.equals(command)
                || StompCommand.SUBSCRIBE.equals(command);
    }

    private String resolveBearerToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getFirstNativeHeader("authorization");
        }
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String trimmed = authHeader.trim();
        String prefix = "bearer ";
        if (trimmed.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            return trimmed.substring(prefix.length()).trim();
        }
        return null;
    }
}

