package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, String> {

    @Query("""
            select r from bizcord_reactions r
            join fetch r.user
            where r.message.id = :messageId
            """)
    List<Reaction> findAllByMessageId(String messageId);

    @Query("""
            select r from bizcord_reactions r
            join fetch r.user
            where r.directMessage.id = :directMessageId
            """)
    List<Reaction> findAllByDirectMessageId(String directMessageId);

    Optional<Reaction> findByEmojiAndUserIdAndMessageId(String emoji, String userId, String messageId);

    Optional<Reaction> findByEmojiAndUserIdAndDirectMessageId(String emoji, String userId, String directMessageId);

    @Query("""
            select r from bizcord_reactions r
            join fetch r.user
            where r.message.id in :messageIds
            """)
    List<Reaction> findAllByMessageIdIn(List<String> messageIds);

    @Query("""
            select r from bizcord_reactions r
            join fetch r.user
            where r.directMessage.id in :directMessageIds
            """)
    List<Reaction> findAllByDirectMessageIdIn(List<String> directMessageIds);
}
