package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Friendship;
import com.bizcord.backend.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, String> {
    @Query("""
            select f from bizcord_friendships f
            join fetch f.requester r
            join fetch f.addressee a
            where (r.id = :userId1 and a.id = :userId2)
               or (r.id = :userId2 and a.id = :userId1)
            """)
    Optional<Friendship> findBetweenUsers(String userId1, String userId2);

    @Query("""
            select f from bizcord_friendships f
            join fetch f.requester r
            join fetch f.addressee a
            where (r.id = :userId or a.id = :userId)
              and f.status = :status
            order by f.updatedAt desc
            """)
    List<Friendship> findAllByUserIdAndStatus(String userId, FriendshipStatus status);

    @Query("""
            select f from bizcord_friendships f
            join fetch f.requester r
            join fetch f.addressee a
            where a.id = :userId
              and f.status = 'PENDING'
            order by f.createdAt desc
            """)
    List<Friendship> findIncomingRequests(String userId);

    @Query("""
            select f from bizcord_friendships f
            join fetch f.requester r
            join fetch f.addressee a
            where r.id = :userId
              and f.status = 'PENDING'
            order by f.createdAt desc
            """)
    List<Friendship> findOutgoingRequests(String userId);
}

