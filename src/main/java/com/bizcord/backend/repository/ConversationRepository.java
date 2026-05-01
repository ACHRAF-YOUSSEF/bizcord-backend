package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("""
            select c from bizcord_conversations c
            join fetch c.user1 u1
            join fetch c.user2 u2
            where (u1.id = :userId or u2.id = :userId)
            and :userId not member of c.deletedByUserIds
            and c.requestStatus = 'ACCEPTED'
            order by c.updatedAt desc
            """)
    List<Conversation> findAllByUserIdOrderByUpdatedAtDesc(String userId);

    @Query("""
            select c from bizcord_conversations c
            join fetch c.user1 u1
            join fetch c.user2 u2
            left join fetch c.requester requester
            where (u1.id = :userId or u2.id = :userId)
            and :userId not member of c.deletedByUserIds
            and c.requestStatus = 'PENDING'
            and requester.id <> :userId
            order by c.updatedAt desc
            """)
    List<Conversation> findMessageRequestsForUser(String userId);

    @Query("""
            select c from bizcord_conversations c
            join fetch c.user1 u1
            join fetch c.user2 u2
            where (u1.id = :userId1 and u2.id = :userId2)
               or (u1.id = :userId2 and u2.id = :userId1)
            """)
    Optional<Conversation> findByUserIds(String userId1, String userId2);

    @Query("""
            select c from bizcord_conversations c
            join fetch c.user1 u1
            join fetch c.user2 u2
            left join fetch c.requester requester
            where c.id = :id
            """)
    Optional<Conversation> findByIdWithUsers(String id);
}
