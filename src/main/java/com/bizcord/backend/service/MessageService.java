package com.bizcord.backend.service;

import com.bizcord.backend.dto.MessageCreateRequest;
import com.bizcord.backend.mapper.MessageMapper;
import com.bizcord.backend.dto.MessageResponse;
import com.bizcord.backend.dto.MessageSearchResponse;
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

import java.time.LocalDateTime;
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
    private final NotificationService notificationService;

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

        if (request.getParentMessageId() != null && !request.getParentMessageId().isBlank()) {
            Message parent = messageRepository.findById(request.getParentMessageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent message not found"));
            message.setParentMessage(parent);
        }

        messageRepository.save(message);

        Message saved = messageRepository.findByIdWithMemberAndUser(message.getId()).orElseThrow();

        List<Reaction> reactions = reactionRepository.findAllByMessageId(saved.getId());
        MessageResponse response = toResponse(saved, reactions, currentUser.getId());
        broadcast(channelId, "NEW", response);

        notificationService.notifyMentions(
                request.getContent(),
                currentUser.getId(),
                channel.getServer().getId(),
                channelId,
                saved.getId()
        );

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

    @Transactional(readOnly = true)
    public List<MessageSearchResponse> searchMessages(String serverId, String channelId, String query, int page, String email) {
        User currentUser = getCurrentUser(email);
        memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.NOT_A_MEMBER));

        String channelFilter = (channelId != null && !channelId.isBlank()) ? channelId : null;
        List<Message> messages = messageRepository.searchByContent(serverId, channelFilter, query, PageRequest.of(page, 20));

        return messages.stream().map(m -> MessageSearchResponse.builder()
                .id(m.getId())
                .content(m.getContent())
                .attachments(m.getAttachments())
                .channelId(m.getChannel().getId())
                .channelName(m.getChannel().getName())
                .createdAt(m.getCreatedAt())
                .member(MessageSearchResponse.MemberItem.builder()
                        .id(m.getMember().getId())
                        .name(m.getMember().getName())
                        .role(m.getMember().getRole())
                        .user(MessageSearchResponse.UserItem.builder()
                                .id(m.getMember().getUser().getId())
                                .fullName(m.getMember().getUser().getFullName())
                                .username(m.getMember().getUser().getUsername2())
                                .imageUrl(m.getMember().getUser().getImageUrl())
                                .status(m.getMember().getUser().getStatus())
                                .build())
                        .build())
                .build()).toList();
    }

    // --- Pin/Unpin ---

    @Transactional
    public MessageResponse togglePin(String channelId, String messageId, String email) {
        User currentUser = getCurrentUser(email);
        Channel channel = getChannelWithServer(channelId);
        Member callerMember = assertMember(channel, currentUser);

        if (callerMember.getRole() != MemberRole.ADMIN && callerMember.getRole() != MemberRole.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins and moderators can pin messages");
        }

        Message message = messageRepository.findByIdWithMemberAndUser(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.MESSAGE_NOT_FOUND));

        if (message.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pin a deleted message");
        }

        boolean wasPinned = message.getPinnedAt() != null;
        if (wasPinned) {
            message.setPinnedAt(null);
            message.setPinnedBy(null);
        } else {
            message.setPinnedAt(LocalDateTime.now());
            message.setPinnedBy(currentUser);
        }
        messageRepository.save(message);

        List<Reaction> reactions = reactionRepository.findAllByMessageId(messageId);
        MessageResponse response = toResponse(message, reactions, currentUser.getId());
        broadcast(channelId, wasPinned ? "UNPIN" : "PIN", response);

        return response;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getPinnedMessages(String channelId, String email) {
        User currentUser = getCurrentUser(email);
        Channel channel = getChannelWithServer(channelId);
        assertMember(channel, currentUser);

        List<Message> pinned = messageRepository.findPinnedByChannelId(channelId);

        List<String> messageIds = pinned.stream().map(Message::getId).toList();
        Map<String, List<Reaction>> reactionsByMessage = reactionRepository.findAllByMessageIdIn(messageIds)
                .stream().collect(Collectors.groupingBy(r -> r.getMessage().getId()));

        return pinned.stream()
                .map(m -> toResponse(m, reactionsByMessage.getOrDefault(m.getId(), List.of()), currentUser.getId()))
                .toList();
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

        MessageResponse response = MessageMapper.INSTANCE.toResponse(msg);
        response.setReactions(reactionGroups);
        if (msg.getParentMessage() != null) {
            Message parent = msg.getParentMessage();
            String senderName = parent.getMember() != null
                    ? parent.getMember().getUser().getUsername2()
                    : "Unknown";
            response.setParentMessage(MessageResponse.ParentMessagePreview.builder()
                    .id(parent.getId())
                    .content(parent.isDeleted() ? null : parent.getContent())
                    .senderName(senderName)
                    .build());
        }
        return response;
    }
}

