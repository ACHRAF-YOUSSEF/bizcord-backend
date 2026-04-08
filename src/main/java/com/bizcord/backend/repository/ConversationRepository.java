package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("""
            select c from bizcord_conversations c
            join fetch c.member1 m1
            join fetch m1.user u1
            join fetch c.member2 m2
            join fetch m2.user u2
            where (m1.user.id = :userId or m2.user.id = :userId)
            and :userId not member of c.deletedByUserIds
            order by c.updatedAt desc
            """)
    List<Conversation> findAllByUserIdOrderByUpdatedAtDesc(String userId);

    @Query("""
            select c from bizcord_conversations c
            join fetch c.member1 m1
            join fetch m1.user u1
            join fetch c.member2 m2
            join fetch m2.user u2
            where (m1.user.id = :userId1 and m2.user.id = :userId2)
               or (m1.user.id = :userId2 and m2.user.id = :userId1)
            """)
    Optional<Conversation> findByUserIds(String userId1, String userId2);

    @Query("""
            select c from bizcord_conversations c
            join fetch c.member1 m1
            join fetch m1.user u1
            join fetch c.member2 m2
            join fetch m2.user u2
            where c.id = :id
            """)
    Optional<Conversation> findByIdWithMembers(@Param("id") String id);
}

