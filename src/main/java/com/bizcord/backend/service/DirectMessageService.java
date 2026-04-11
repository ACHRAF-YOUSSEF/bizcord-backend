package com.bizcord.backend.service;

import com.bizcord.backend.dto.DirectMessageCreateRequest;
import com.bizcord.backend.dto.DirectMessageResponse;
import com.bizcord.backend.dto.DirectMessageUpdateRequest;
import com.bizcord.backend.dto.WebSocketMessage;
import com.bizcord.backend.entity.Conversation;
import com.bizcord.backend.entity.DirectMessage;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.ConversationRepository;
import com.bizcord.backend.repository.DirectMessageRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectMessageService {
    private static final int PAGE_SIZE = 20;

    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<DirectMessageResponse> getMessages(String conversationId, String cursor, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = getConversationWithUsers(conversationId);
        assertParticipant(conversation, currentUser);

        List<DirectMessage> messages;
        PageRequest page = PageRequest.of(0, PAGE_SIZE);

        if (cursor != null && !cursor.isBlank()) {
            DirectMessage cursorMessage = directMessageRepository.findById(cursor)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.DIRECT_MESSAGE_NOT_FOUND));
            messages = directMessageRepository.findByConversationIdBeforeCursor(
                    conversationId, cursorMessage.getCreatedAt(), page);
        } else {
            messages = directMessageRepository.findLatestByConversationId(conversationId, page);
        }

        List<DirectMessage> reversed = new ArrayList<>(messages).reversed();

        return reversed.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DirectMessageResponse createMessage(String conversationId, DirectMessageCreateRequest request, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = getConversationWithUsers(conversationId);
        assertParticipant(conversation, currentUser);

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();

        if (!hasContent && !hasAttachments) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.DIRECT_MESSAGE_EMPTY);
        }

        DirectMessage message = DirectMessage.builder()
                .content(request.getContent())
                .attachments(hasAttachments ? request.getAttachments() : new ArrayList<>())
                .user(currentUser)
                .conversation(conversation)
                .build();

        directMessageRepository.save(message);

        DirectMessage saved = directMessageRepository.findByIdWithUser(message.getId())
                .orElseThrow();

        DirectMessageResponse response = toResponse(saved);
        broadcast(conversationId, "NEW", response);

        return response;
    }

    @Transactional
    public DirectMessageResponse updateMessage(String conversationId, String messageId, DirectMessageUpdateRequest request, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = getConversationWithUsers(conversationId);
        assertParticipant(conversation, currentUser);

        DirectMessage message = directMessageRepository.findByIdWithUser(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.DIRECT_MESSAGE_NOT_FOUND));

        if (!message.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.DIRECT_MESSAGE_NOT_OWNER);
        }

        message.setContent(request.getContent());
        directMessageRepository.save(message);

        DirectMessageResponse response = toResponse(message);
        broadcast(conversationId, "UPDATE", response);

        return response;
    }

    @Transactional
    public DirectMessageResponse deleteMessage(String conversationId, String messageId, String email) {
        User currentUser = getCurrentUser(email);
        Conversation conversation = getConversationWithUsers(conversationId);
        assertParticipant(conversation, currentUser);

        DirectMessage message = directMessageRepository.findByIdWithUser(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.DIRECT_MESSAGE_NOT_FOUND));

        if (!message.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.DIRECT_MESSAGE_NOT_OWNER);
        }

        message.setDeleted(true);
        message.setContent(null);
        message.setAttachments(new ArrayList<>());
        directMessageRepository.save(message);

        DirectMessageResponse response = toResponse(message);
        broadcast(conversationId, "DELETE", response);

        return response;
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private Conversation getConversationWithUsers(String conversationId) {
        return conversationRepository.findByIdWithUsers(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND));
    }

    private void assertParticipant(Conversation conversation, User user) {
        boolean isParticipant = conversation.getUser1().getId().equals(user.getId())
                || conversation.getUser2().getId().equals(user.getId());
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CONVERSATION_NOT_A_PARTICIPANT);
        }
    }

    private void broadcast(String conversationId, String type, DirectMessageResponse data) {
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                new WebSocketMessage(type, data));
    }

    private DirectMessageResponse toResponse(DirectMessage dm) {
        User user = dm.getUser();

        return DirectMessageResponse.builder()
                .id(dm.getId())
                .content(dm.getContent())
                .attachments(dm.getAttachments())
                .user(DirectMessageResponse.UserItem.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .username(user.getUsername2())
                        .email(user.getEmail())
                        .imageUrl(user.getImageUrl())
                        .build())
                .conversationId(dm.getConversation().getId())
                .deleted(dm.isDeleted())
                .createdAt(dm.getCreatedAt())
                .updatedAt(dm.getUpdatedAt())
                .build();
    }
}

