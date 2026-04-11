package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, String> {

    @Query("""
            select e from bizcord_events e
            join fetch e.creator
            join fetch e.server
            where e.id = :eventId
            """)
    Optional<Event> findByIdWithCreatorAndServer(String eventId);

    @Query("""
            select e from bizcord_events e
            join fetch e.creator
            where e.server.id = :serverId
            order by e.startTime asc
            """)
    List<Event> findAllByServerIdOrderByStartTime(String serverId);

    @Query("""
            select e from bizcord_events e
            join fetch e.creator
            where e.server.id = :serverId
            and e.startTime >= :from and e.startTime < :to
            order by e.startTime asc
            """)
    List<Event> findAllByServerIdAndStartTimeBetween(String serverId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select e from bizcord_events e
            join fetch e.creator
            where e.server.id = :serverId
            and e.startTime >= :now
            order by e.startTime asc
            """)
    List<Event> findUpcomingByServerId(String serverId, LocalDateTime now);
}
