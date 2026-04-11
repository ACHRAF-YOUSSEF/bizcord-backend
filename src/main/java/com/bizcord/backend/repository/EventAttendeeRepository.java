package com.bizcord.backend.repository;

import com.bizcord.backend.entity.EventAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventAttendeeRepository extends JpaRepository<EventAttendee, String> {

    @Query("""
            select ea from bizcord_event_attendees ea
            join fetch ea.user
            where ea.event.id = :eventId
            """)
    List<EventAttendee> findAllByEventIdWithUser(String eventId);

    @Query("""
            select ea from bizcord_event_attendees ea
            join fetch ea.user
            where ea.event.id in :eventIds
            """)
    List<EventAttendee> findAllByEventIdInWithUser(List<String> eventIds);

    Optional<EventAttendee> findByEventIdAndUserId(String eventId, String userId);
}
