package com.bizcord.backend.repository;

import com.bizcord.backend.entity.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {
    @Query("""
            select dm from bizcord_direct_messages dm
            join fetch dm.user u
            left join fetch dm.parentMessage pdm
            left join fetch pdm.user pu
            where dm.conversation.id = :conversationId
            order by dm.createdAt desc
            """)
    List<DirectMessage> findLatestByConversationId(String conversationId, Pageable pageable);

    @Query("""
            select dm from bizcord_direct_messages dm
            join fetch dm.user u
            left join fetch dm.parentMessage pdm
            left join fetch pdm.user pu
            where dm.conversation.id = :conversationId
              and dm.createdAt < :cursor
            order by dm.createdAt desc
            """)
    List<DirectMessage> findByConversationIdBeforeCursor(String conversationId, LocalDateTime cursor, Pageable pageable);

    @Query("""
            select dm from bizcord_direct_messages dm
            join fetch dm.user u
            left join fetch dm.parentMessage pdm
            left join fetch pdm.user pu
            where dm.id = :id
            """)
    Optional<DirectMessage> findByIdWithUser(String id);

    @Query("""
            select dm from bizcord_direct_messages dm
            join fetch dm.user u
            where dm.conversation.id = :conversationId
              and dm.deleted = false
              and lower(dm.content) like lower(concat('%', :query, '%'))
            order by dm.createdAt desc
            """)
    List<DirectMessage> searchByContent(String conversationId, String query, Pageable pageable);

    @Query("""
            select dm from bizcord_direct_messages dm
            join fetch dm.user u
            left join fetch dm.pinnedBy pb
            left join fetch dm.parentMessage pdm
            left join fetch pdm.user pu
            where dm.conversation.id = :conversationId
              and dm.pinnedAt is not null
            order by dm.pinnedAt desc
            """)
    List<DirectMessage> findPinnedByConversationId(String conversationId);
}

