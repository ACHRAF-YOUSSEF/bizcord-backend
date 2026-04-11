package com.bizcord.backend.service;

import com.bizcord.backend.dto.*;
import com.bizcord.backend.entity.*;
import com.bizcord.backend.repository.*;
import com.bizcord.backend.utils.ErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<EventResponse> getEvents(String serverId, String email) {
        User user = findUser(email);
        assertMember(serverId, user);

        List<Event> events = eventRepository.findAllByServerIdOrderByStartTime(serverId);
        return toResponseList(events, user.getId());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents(String serverId, String email) {
        User user = findUser(email);
        assertMember(serverId, user);

        List<Event> events = eventRepository.findUpcomingByServerId(serverId, LocalDateTime.now());
        return toResponseList(events, user.getId());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByMonth(String serverId, int year, int month, String email) {
        User user = findUser(email);
        assertMember(serverId, user);

        LocalDateTime from = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime to = from.plusMonths(1);
        List<Event> events = eventRepository.findAllByServerIdAndStartTimeBetween(serverId, from, to);
        return toResponseList(events, user.getId());
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(String serverId, String eventId, String email) {
        User user = findUser(email);
        assertMember(serverId, user);

        Event event = findEventInServer(eventId, serverId);
        List<EventAttendee> attendees = attendeeRepository.findAllByEventIdWithUser(eventId);
        return toResponse(event, attendees);
    }

    @Transactional
    public EventResponse createEvent(String serverId, EventCreateRequest request, String email) {
        User user = findUser(email);
        Member member = assertMember(serverId, user);

        if (member.getRole() == MemberRole.GUEST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.EVENT_CREATE_FORBIDDEN);
        }

        if (request.getEndTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.EVENT_INVALID_TIME);
        }

        Server server = serverRepository.findByIdWithOwner(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.SERVER_NOT_FOUND));

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .color(request.getColor())
                .server(server)
                .creator(user)
                .build();

        eventRepository.save(event);

        EventResponse response = toResponse(event, List.of());
        broadcast(serverId, "EVENT_CREATED", response);
        notificationService.notifyEventCreated(event);
        return response;
    }

    @Transactional
    public EventResponse updateEvent(String serverId, String eventId, EventUpdateRequest request, String email) {
        User user = findUser(email);
        Member member = assertMember(serverId, user);
        Event event = findEventInServer(eventId, serverId);

        if (!event.getCreator().getId().equals(user.getId()) && member.getRole() != MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.EVENT_EDIT_FORBIDDEN);
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getColor() != null) {
            event.setColor(request.getColor());
        }

        if (event.getEndTime() != null && event.getEndTime().isBefore(event.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.EVENT_INVALID_TIME);
        }

        eventRepository.save(event);

        List<EventAttendee> attendees = attendeeRepository.findAllByEventIdWithUser(eventId);
        EventResponse response = toResponse(event, attendees);
        broadcast(serverId, "EVENT_UPDATED", response);
        return response;
    }

    @Transactional
    public void deleteEvent(String serverId, String eventId, String email) {
        User user = findUser(email);
        Member member = assertMember(serverId, user);
        Event event = findEventInServer(eventId, serverId);

        if (!event.getCreator().getId().equals(user.getId()) && member.getRole() != MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.EVENT_DELETE_FORBIDDEN);
        }

        eventRepository.delete(event);
        broadcast(serverId, "EVENT_DELETED", EventResponse.builder().id(eventId).serverId(serverId).build());
    }

    @Transactional
    public EventResponse rsvp(String serverId, String eventId, EventRsvpRequest request, String email) {
        User user = findUser(email);
        assertMember(serverId, user);
        Event event = findEventInServer(eventId, serverId);

        EventAttendee attendee = attendeeRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElse(null);

        if (attendee != null) {
            attendee.setStatus(request.getStatus());
            attendeeRepository.save(attendee);
        } else {
            attendee = EventAttendee.builder()
                    .event(event)
                    .user(user)
                    .status(request.getStatus())
                    .build();
            attendeeRepository.save(attendee);
        }

        List<EventAttendee> attendees = attendeeRepository.findAllByEventIdWithUser(eventId);
        EventResponse response = toResponse(event, attendees);
        broadcast(serverId, "EVENT_RSVP", response);
        notificationService.notifyRsvpUpdate(event, user, request.getStatus());
        return response;
    }

    @Transactional
    public EventResponse removeRsvp(String serverId, String eventId, String email) {
        User user = findUser(email);
        assertMember(serverId, user);
        findEventInServer(eventId, serverId);

        attendeeRepository.findByEventIdAndUserId(eventId, user.getId())
                .ifPresent(attendeeRepository::delete);

        Event event = findEventInServer(eventId, serverId);
        List<EventAttendee> attendees = attendeeRepository.findAllByEventIdWithUser(eventId);
        EventResponse response = toResponse(event, attendees);
        broadcast(serverId, "EVENT_RSVP", response);
        return response;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.USER_NOT_FOUND));
    }

    private Member assertMember(String serverId, User user) {
        return memberRepository.findByServerIdAndUserIdWithUserAndServer(serverId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.NOT_A_MEMBER));
    }

    private Event findEventInServer(String eventId, String serverId) {
        Event event = eventRepository.findByIdWithCreatorAndServer(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessages.EVENT_NOT_FOUND));
        if (!event.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.EVENT_WRONG_SERVER);
        }
        return event;
    }

    private void broadcast(String serverId, String type, EventResponse data) {
        messagingTemplate.convertAndSend(
                "/topic/servers/" + serverId + "/events",
                new WebSocketMessage(type, data));
    }

    private List<EventResponse> toResponseList(List<Event> events, String currentUserId) {
        if (events.isEmpty()) return List.of();

        List<String> eventIds = events.stream().map(Event::getId).toList();
        List<EventAttendee> allAttendees = attendeeRepository.findAllByEventIdInWithUser(eventIds);

        return events.stream().map(event -> {
            List<EventAttendee> eventAttendees = allAttendees.stream()
                    .filter(a -> a.getEvent().getId().equals(event.getId()))
                    .toList();
            return toResponse(event, eventAttendees);
        }).toList();
    }

    private EventResponse toResponse(Event event, List<EventAttendee> attendees) {
        User creator = event.getCreator();
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .location(event.getLocation())
                .color(event.getColor())
                .serverId(event.getServer().getId())
                .creator(EventResponse.CreatorItem.builder()
                        .id(creator.getId())
                        .fullName(creator.getFullName())
                        .username(creator.getUsername2())
                        .imageUrl(creator.getImageUrl())
                        .build())
                .attendees(attendees.stream().map(a -> EventResponse.AttendeeItem.builder()
                        .id(a.getId())
                        .userId(a.getUser().getId())
                        .fullName(a.getUser().getFullName())
                        .username(a.getUser().getUsername2())
                        .imageUrl(a.getUser().getImageUrl())
                        .status(a.getStatus())
                        .build()).toList())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
