package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.*;
import com.bizcord.backend.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/servers/{serverId}/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(
            @PathVariable String serverId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.getEvents(serverId, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @PathVariable String serverId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.getUpcomingEvents(serverId, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/month")
    public ResponseEntity<List<EventResponse>> getEventsByMonth(
            @PathVariable String serverId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.getEventsByMonth(serverId, year, month, userDetails.getUsername()));
    }

    @RateLimit(limit = 30, keyType = RateLimitKeyType.UID)
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable String serverId,
            @PathVariable String eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.getEvent(serverId, eventId, userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @PathVariable String serverId,
            @Valid @RequestBody EventCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(CREATED)
                .body(eventService.createEvent(serverId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PatchMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable String serverId,
            @PathVariable String eventId,
            @Valid @RequestBody EventUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.updateEvent(serverId, eventId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String serverId,
            @PathVariable String eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        eventService.deleteEvent(serverId, eventId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @RateLimit(limit = 20, keyType = RateLimitKeyType.UID)
    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<EventResponse> rsvp(
            @PathVariable String serverId,
            @PathVariable String eventId,
            @Valid @RequestBody EventRsvpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.rsvp(serverId, eventId, request, userDetails.getUsername()));
    }

    @RateLimit(limit = 20, keyType = RateLimitKeyType.UID)
    @DeleteMapping("/{eventId}/rsvp")
    public ResponseEntity<EventResponse> removeRsvp(
            @PathVariable String serverId,
            @PathVariable String eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.removeRsvp(serverId, eventId, userDetails.getUsername()));
    }
}
