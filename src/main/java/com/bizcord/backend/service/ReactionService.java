package com.bizcord.backend.service;

import com.bizcord.backend.dto.WebSocketMessage;
import com.bizcord.backend.entity.*;
import com.bizcord.backend.repository.*;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final DirectMessageRepository directMessageRepository;
    private final ChannelRepository channelRepository;
    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void toggleChannelMessageReaction(String channelId, String messageId, String emoji, String email) {
        User user = getUser(email);
        Channel channel = channelRepository.findByIdWithServer(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CHANNEL_NOT_FOUND));
        memberRepository.findByServerIdAndUserIdWithUserAndServer(channel.getServer().getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.NOT_A_MEMBER));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MESSAGE_NOT_FOUND));

        Optional<Reaction> existing = reactionRepository.findByEmojiAndUserIdAndMessageId(emoji, user.getId(), messageId);

        String eventType;
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            eventType = "REACTION_REMOVE";
        } else {
            Reaction reaction = Reaction.builder()
                    .emoji(emoji)
                    .user(user)
                    .message(message)
                    .build();
            reactionRepository.save(reaction);
            eventType = "REACTION_ADD";
        }

        ReactionEvent event = ReactionEvent.builder()
                .messageId(messageId)
                .emoji(emoji)
                .userId(user.getId())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/channels/" + channelId,
                new WebSocketMessage(eventType, event));
    }

    @Transactional
    public void toggleDirectMessageReaction(String conversationId, String messageId, String emoji, String email) {
        User user = getUser(email);
        Conversation conversation = conversationRepository.findByIdWithUsers(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND));

        boolean isParticipant = conversation.getUser1().getId().equals(user.getId())
                || conversation.getUser2().getId().equals(user.getId());
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CONVERSATION_NOT_A_PARTICIPANT);
        }

        DirectMessage dm = directMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.DIRECT_MESSAGE_NOT_FOUND));

        Optional<Reaction> existing = reactionRepository.findByEmojiAndUserIdAndDirectMessageId(emoji, user.getId(), messageId);

        String eventType;
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            eventType = "REACTION_REMOVE";
        } else {
            Reaction reaction = Reaction.builder()
                    .emoji(emoji)
                    .user(user)
                    .directMessage(dm)
                    .build();
            reactionRepository.save(reaction);
            eventType = "REACTION_ADD";
        }

        ReactionEvent event = ReactionEvent.builder()
                .messageId(messageId)
                .emoji(emoji)
                .userId(user.getId())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                new WebSocketMessage(eventType, event));
    }

    public List<Reaction> getReactionsForMessages(List<String> messageIds) {
        if (messageIds.isEmpty()) return List.of();
        return reactionRepository.findAllByMessageIdIn(messageIds);
    }

    public List<Reaction> getReactionsForDirectMessages(List<String> directMessageIds) {
        if (directMessageIds.isEmpty()) return List.of();
        return reactionRepository.findAllByDirectMessageIdIn(directMessageIds);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    @Data
    @Builder
    public static class ReactionEvent {
        private String messageId;
        private String emoji;
        private String userId;
    }
}
