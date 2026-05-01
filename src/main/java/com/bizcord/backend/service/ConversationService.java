package com.bizcord.backend.service;

import com.bizcord.backend.dto.ConversationResponse;
import com.bizcord.backend.entity.Conversation;
import com.bizcord.backend.entity.ConversationRequestStatus;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.mapper.ConversationMapper;
import com.bizcord.backend.repository.ConversationRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;
    private final FriendshipService friendshipService;

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMessageRequests(String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        return conversationRepository.findMessageRequestsForUser(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(String conversationId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Conversation conversation = conversationRepository.findByIdWithUsers(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND));

        assertParticipant(conversation, currentUser);

        if (conversation.getDeletedByUserIds().contains(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND);
        }

        return toResponse(conversation);
    }

    @Transactional
    public ConversationResponse getOrCreateConversation(String targetUserId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        if (currentUser.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.CONVERSATION_SELF_NOT_ALLOWED);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.USER_NOT_FOUND));

        Optional<Conversation> existing = conversationRepository.findByUserIds(currentUser.getId(), targetUser.getId());
        if (existing.isPresent()) {
            Conversation conv = existing.get();
            if (conv.getDeletedByUserIds().remove(currentUser.getId())) {
                conversationRepository.save(conv);
                conv = conversationRepository.findByIdWithUsers(conv.getId()).orElseThrow();
            }
            return toResponse(conv);
        }

        boolean accepted = friendshipService.areFriends(currentUser.getId(), targetUser.getId());
        Conversation conversation = Conversation.builder()
                .user1(currentUser)
                .user2(targetUser)
                .requester(accepted ? null : currentUser)
                .requestStatus(accepted ? ConversationRequestStatus.ACCEPTED : ConversationRequestStatus.PENDING)
                .build();

        conversationRepository.save(conversation);

        Conversation saved = conversationRepository.findByIdWithUsers(conversation.getId())
                .orElseThrow();

        return toResponse(saved);
    }

    @Transactional
    public ConversationResponse acceptMessageRequest(String conversationId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Conversation conversation = conversationRepository.findByIdWithUsers(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND));

        assertParticipant(conversation, currentUser);
        if (conversation.getRequester() != null && conversation.getRequester().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CONVERSATION_REQUEST_FORBIDDEN);
        }

        conversation.setRequestStatus(ConversationRequestStatus.ACCEPTED);
        conversation.setRequester(null);
        conversation.getDeletedByUserIds().remove(currentUser.getId());
        return toResponse(conversationRepository.save(conversation));
    }

    @Transactional
    public void deleteConversation(String conversationId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));

        Conversation conversation = conversationRepository.findByIdWithUsers(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CONVERSATION_NOT_FOUND));

        assertParticipant(conversation, currentUser);

        conversation.getDeletedByUserIds().add(currentUser.getId());
        conversationRepository.save(conversation);
    }

    private void assertParticipant(Conversation conversation, User user) {
        boolean isParticipant = conversation.getUser1().getId().equals(user.getId())
                || conversation.getUser2().getId().equals(user.getId());
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.CONVERSATION_NOT_A_PARTICIPANT);
        }
    }

    private ConversationResponse toResponse(Conversation c) {
        return conversationMapper.toResponse(c);
    }
}
