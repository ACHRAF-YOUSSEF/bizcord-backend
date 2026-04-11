package com.bizcord.backend.repository;

import com.bizcord.backend.entity.BannedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BannedUserRepository extends JpaRepository<BannedUser, String> {
    boolean existsByServerIdAndUserId(String serverId, String userId);

    Optional<BannedUser> findByServerIdAndUserId(String serverId, String userId);

    @Query("""
            select b from bizcord_banned_users b
            join fetch b.user
            where b.server.id = :serverId
            """)
    List<BannedUser> findAllByServerIdWithUser(String serverId);
}
