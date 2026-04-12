package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, String> {

    @Query("""
            select m from bizcord_messages m
            join fetch m.member mb
            join fetch mb.user
            left join fetch m.parentMessage pm
            left join fetch pm.member pmb
            left join fetch pmb.user
            where m.channel.id = :channelId
            order by m.createdAt desc
            """)
    List<Message> findLatestByChannelId(String channelId, Pageable pageable);

    @Query("""
            select m from bizcord_messages m
            join fetch m.member mb
            join fetch mb.user
            left join fetch m.parentMessage pm
            left join fetch pm.member pmb
            left join fetch pmb.user
            where m.channel.id = :channelId
              and m.createdAt < :cursor
            order by m.createdAt desc
            """)
    List<Message> findByChannelIdBeforeCursor(String channelId, LocalDateTime cursor, Pageable pageable);

    @Query("""
            select m from bizcord_messages m
            join fetch m.member mb
            join fetch mb.user
            left join fetch m.parentMessage pm
            left join fetch pm.member pmb
            left join fetch pmb.user
            where m.id = :id
            """)
    Optional<Message> findByIdWithMemberAndUser(String id);

    @Query("""
            select m from bizcord_messages m
            join fetch m.member mb
            join fetch mb.user
            where m.channel.server.id = :serverId
              and (:channelId is null or m.channel.id = :channelId)
              and lower(m.content) like lower(concat('%', :query, '%'))
              and m.deleted = false
            order by m.createdAt desc
            """)
    List<Message> searchByContent(String serverId, String channelId, String query, Pageable pageable);
}

