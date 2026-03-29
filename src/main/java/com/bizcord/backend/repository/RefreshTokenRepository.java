package com.bizcord.backend.repository;

import com.bizcord.backend.entity.RefreshToken;
import com.bizcord.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM refresh_tokens rt WHERE rt.user = :user")
    void deleteAllByUser(User user);
}

