package com.bizcord.backend.service;

import com.bizcord.backend.dto.MessageCreateRequest;
import com.bizcord.backend.dto.MessageResponse;
import com.bizcord.backend.dto.MessageUpdateRequest;
import com.bizcord.backend.dto.WebSocketMessage;
import com.bizcord.backend.entity.Channel;
import com.bizcord.backend.entity.Member;
import com.bizcord.backend.entity.MemberRole;
import com.bizcord.backend.entity.Message;
import com.bizcord.backend.entity.Reaction;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.ChannelRepository;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.MessageRepository;
import com.bizcord.backend.repository.ReactionRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    private static final int PAGE_SIZE = 50;

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ReactionRepository reactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String channelId, String cursor, String email) {
        User currentUser = getCurrentUser(email);
        Channel channel = getChannelWithServer(channelId);
        Member _ = assertMember(channel, currentUser);

        List<Message> messages;
        PageRequest page = PageRequest.of(0, PAGE_SIZE);

        if (cursor != null && !cursor.isBlank()) {
            Message cursorMessage = messageRepository.findById(cursor)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MESSAGE_NOT_FOUND));
            messages = messageRepository.findByChannelIdBeforeCursor(channelId, cursorMessage.getCreatedAt(), page);
        } else {
            messages = messageRepository.findLatestByChannelId(channelId, page);
        }

        // reverse descending list to return ascending order
        List<Message> sorted = new ArrayList<>(messages).reversed();

        List<String> messageIds = sorted.stream().map(Message::getId).toList();
        Map<String, List<Reaction>> reactionsByMessage = reactionRepository.findAllByMessageIdIn(messageIds)
                .stream().collect(Collectors.groupingBy(r -> r.getMessage().getId()));

        return sorted.stream().map(m -> toResponse(m, reactionsByMessage.getOrDefault(m.getId(), List.of()), currentUser.getId())).toList();
    }

    @Transactional
    public MessageResponse createMessage(String channelId, MessageCreateRequest request, String email) {
        User currentUser = getCurrentUser(email);
        Channel channel = getChannelWithServer(channelId);
        Member callerMember = assertMember(channel, currentUser);

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();

        if (!hasContent && !hasAttachments) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.MESSAGE_EMPTY);
        }

        Message message = Message.builder()
                .content(request.getContent())
                .attachments(hasAttachments ? request.getAttachments() : new ArrayList<>())
                .member(callerMember)
                .channel(channel)
                .build();

        messageRepository.save(message);

        Message saved = messageRepository.findByIdWithMemberAndUser(message.getId()).orElseThrow();

        List<Reaction> reactions = reactionRepository.findAllByMessageId(saved.getId());
        MessageResponse response = toResponse(saved, reactions, currentUser.getId());
        broadcast(channelId, "NEW", response);

        return response;
    }

    @Transactional
    public MessageResponse updateMessage(String channelId, String messageId, MessageUpdateRequest request, String email) {
        User currentUser = getCurrentUser(email);
        Channel channel = getChannelWithServer(channelId);
        assertMember(channel, currentUser);

        Message message = messageRepository.findByIdWithMemberAndUser(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MESSAGE_NOT_FOUND));

        if (!message.getMember().getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MESSAGE_NOT_OWNER);
        }

        message.setContent(request.getContent());
        messageRepository.save(message);

        List<Reaction> reactions = reactionRepository.findAllByMessageId(messageId);
        MessageResponse response = toResponse(message, reactions, currentUser.getId());
        broadcast(channelId, "UPDATE", response);

        return response;
    }

    @Transactional
    public MessageResponse deleteMessage(String channelId, String messageId, String email) {
        User currentUser = getCurrentUser(email);
        Channel channel = getChannelWithServer(channelId);
        Member callerMember = assertMember(channel, currentUser);

        Message message = messageRepository.findByIdWithMemberAndUser(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MESSAGE_NOT_FOUND));

        boolean isAuthor = message.getMember().getUser().getId().equals(currentUser.getId());
        boolean isAdminOrModerator = callerMember.getRole() == MemberRole.ADMIN
                || callerMember.getRole() == MemberRole.MODERATOR;

        if (!isAuthor && !isAdminOrModerator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.MESSAGE_DELETE_FORBIDDEN);
        }

        message.setDeleted(true);
        message.setContent(null);
        message.setAttachments(new ArrayList<>());
        messageRepository.save(message);

        MessageResponse response = toResponse(message, List.of(), currentUser.getId());
        broadcast(channelId, "DELETE", response);

        return response;
    }

    // --- helpers ---

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private Channel getChannelWithServer(String channelId) {
        return channelRepository.findByIdWithServer(channelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.CHANNEL_NOT_FOUND));
    }

    private Member assertMember(Channel channel, User user) {
        return memberRepository.findByServerIdAndUserIdWithUserAndServer(channel.getServer().getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.NOT_A_MEMBER));
    }

    private void broadcast(String channelId, String type, MessageResponse data) {
        messagingTemplate.convertAndSend(
                "/topic/channels/" + channelId,
                new WebSocketMessage(type, data));
    }

    private MessageResponse toResponse(Message msg, List<Reaction> reactions, String currentUserId) {
        Member member = msg.getMember();
        User user = member.getUser();

        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        List<MessageResponse.ReactionGroup> reactionGroups = grouped.entrySet().stream()
                .map(e -> MessageResponse.ReactionGroup.builder()
                        .emoji(e.getKey())
                        .count(e.getValue().size())
                        .userIds(e.getValue().stream().map(r -> r.getUser().getId()).toList())
                        .me(e.getValue().stream().anyMatch(r -> r.getUser().getId().equals(currentUserId)))
                        .build())
                .toList();

        return MessageResponse.builder()
                .id(msg.getId())
                .content(msg.getContent())
                .attachments(msg.getAttachments())
                .member(MessageResponse.MemberItem.builder()
                        .id(member.getId())
                        .name(member.getName())
                        .role(member.getRole())
                        .user(MessageResponse.UserItem.builder()
                                .id(user.getId())
                                .fullName(user.getFullName())
                                .username(user.getUsername2())
                                .imageUrl(user.getImageUrl())
                                .build())
                        .build())
                .channelId(msg.getChannel().getId())
                .deleted(msg.isDeleted())
                .reactions(reactionGroups)
                .createdAt(msg.getCreatedAt())
                .updatedAt(msg.getUpdatedAt())
                .build();
    }
}

