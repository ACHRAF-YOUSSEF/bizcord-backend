package com.bizcord.backend.service;

import com.bizcord.backend.dto.NotificationResponse;
import com.bizcord.backend.dto.WebSocketMessage;
import com.bizcord.backend.entity.*;
import com.bizcord.backend.repository.MemberRepository;
import com.bizcord.backend.repository.NotificationRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final int PAGE_SIZE = 30;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // --- Query operations ---

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String cursor, String email) {
        User user = findUser(email);
        List<Notification> notifications;

        if (cursor != null && !cursor.isBlank()) {
            Notification cursorNotif = notificationRepository.findById(cursor)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOTIFICATION_NOT_FOUND));
            notifications = notificationRepository.findAllByUserIdBeforeCursor(
                    user.getId(), cursorNotif.getCreatedAt(), PageRequest.of(0, PAGE_SIZE));
        } else {
            notifications = notificationRepository.findAllByUserIdOrdered(
                    user.getId(), PageRequest.of(0, PAGE_SIZE));
        }

        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = findUser(email);
        return notificationRepository.countUnreadByUserId(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(String notificationId, String email) {
        User user = findUser(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.NOTIFICATION_FORBIDDEN);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(String email) {
        User user = findUser(email);
        notificationRepository.markAllReadByUserId(user.getId());
    }

    @Transactional
    public void deleteNotification(String notificationId, String email) {
        User user = findUser(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.NOTIFICATION_FORBIDDEN);
        }

        notificationRepository.delete(notification);
    }

    // --- Notification generators (called from other services) ---

    @Transactional
    public void notifyMentions(String content, String authorUserId, String serverId, String channelId, String messageId) {
        if (content == null || content.isBlank()) return;

        List<Member> serverMembers = memberRepository.findAllByServerIdWithUserAndServer(serverId);
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            String mentionedUsername = matcher.group(1);
            serverMembers.stream()
                    .filter(m -> m.getUser().getUsername2().equalsIgnoreCase(mentionedUsername))
                    .filter(m -> !m.getUser().getId().equals(authorUserId))
                    .findFirst()
                    .ifPresent(member -> {
                        String authorName = serverMembers.stream()
                                .filter(m -> m.getUser().getId().equals(authorUserId))
                                .map(m -> m.getUser().getUsername2())
                                .findFirst().orElse("Someone");

                        createAndPush(
                                member.getUser(),
                                NotificationType.MENTION,
                                authorName + " mentioned you",
                                truncate(content, 100),
                                messageId,
                                ReferenceType.MESSAGE,
                                serverId,
                                channelId
                        );
                    });
        }
    }

    @Transactional
    public void notifyEventCreated(Event event) {
        List<Member> members = memberRepository.findAllByServerIdWithUserAndServer(event.getServer().getId());
        String creatorName = event.getCreator().getUsername2();

        for (Member member : members) {
            if (member.getUser().getId().equals(event.getCreator().getId())) continue;

            createAndPush(
                    member.getUser(),
                    NotificationType.EVENT_CREATED,
                    "New event: " + event.getTitle(),
                    creatorName + " created an event in " + event.getServer().getName(),
                    event.getId(),
                    ReferenceType.EVENT,
                    event.getServer().getId(),
                    null
            );
        }
    }

    @Transactional
    public void notifyRsvpUpdate(Event event, User responder, AttendeeStatus status) {
        if (event.getCreator().getId().equals(responder.getId())) return;

        createAndPush(
                event.getCreator(),
                NotificationType.RSVP_UPDATE,
                responder.getUsername2() + " RSVP'd " + status.name().toLowerCase().replace("_", " "),
                "For event: " + event.getTitle(),
                event.getId(),
                ReferenceType.EVENT,
                event.getServer().getId(),
                null
        );
    }

    // --- Internal helpers ---

    private void createAndPush(User user, NotificationType type, String title, String body,
                               String referenceId, ReferenceType referenceType,
                               String serverId, String channelId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .serverId(serverId)
                .channelId(channelId)
                .build();

        notificationRepository.save(notification);

        NotificationResponse response = toResponse(notification);
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                new WebSocketMessage("NOTIFICATION", response)
        );
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .serverId(n.getServerId())
                .channelId(n.getChannelId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "…";
    }
}
