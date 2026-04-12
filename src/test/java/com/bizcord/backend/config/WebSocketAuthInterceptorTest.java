package com.bizcord.backend.config;

import com.bizcord.backend.config.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    private static final MessageChannel TEST_CHANNEL = new MessageChannel() {
        @Override
        public boolean send(Message<?> message) {
            return true;
        }

        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    };

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService);
    }

    @Test
    void preSend_setsUserOnValidConnectAuthorizationHeader() {
        String jwt = "valid.jwt";
        String email = "user@example.com";
        UserDetails userDetails = User.withUsername(email)
                .password("pw")
                .authorities("ROLE_USER")
                .build();

        when(jwtService.extractUsername(jwt)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);

        Message<byte[]> input = connectMessage("Authorization", "Bearer " + jwt);
        Message<?> result = interceptor.preSend(input, TEST_CHANNEL);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertEquals(email, accessor.getUser().getName());
    }

    @Test
    void preSend_doesNotThrowWhenJwtParsingFails() {
        String jwt = "broken.jwt";
        when(jwtService.extractUsername(jwt)).thenThrow(new IllegalArgumentException("bad token"));

        Message<byte[]> input = connectMessage("Authorization", "Bearer " + jwt);
        Message<?> result = interceptor.preSend(input, TEST_CHANNEL);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNull(accessor.getUser());
        assertSame(input, result);
    }

    @Test
    void preSend_acceptsLowercaseAuthorizationHeaderName() {
        String jwt = "valid.jwt";
        String email = "lower@example.com";
        UserDetails userDetails = User.withUsername(email)
                .password("pw")
                .authorities("ROLE_USER")
                .build();

        when(jwtService.extractUsername(jwt)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);

        Message<byte[]> input = connectMessage("authorization", "Bearer " + jwt);
        Message<?> result = interceptor.preSend(input, TEST_CHANNEL);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertEquals(email, accessor.getUser().getName());
    }

    @Test
    void preSend_setsUserOnSendFrameWhenMissingPrincipal() {
        String jwt = "valid.jwt";
        String email = "send@example.com";
        UserDetails userDetails = User.withUsername(email)
                .password("pw")
                .authorities("ROLE_USER")
                .build();

        when(jwtService.extractUsername(jwt)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);

        Message<byte[]> input = messageWithHeader(StompCommand.SEND, "Authorization", "Bearer " + jwt);
        Message<?> result = interceptor.preSend(input, TEST_CHANNEL);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertEquals(email, accessor.getUser().getName());
    }

    private Message<byte[]> connectMessage(String headerName, String headerValue) {
        return messageWithHeader(StompCommand.CONNECT, headerName, headerValue);
    }

    private Message<byte[]> messageWithHeader(StompCommand command, String headerName, String headerValue) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setNativeHeader(headerName, headerValue);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}


