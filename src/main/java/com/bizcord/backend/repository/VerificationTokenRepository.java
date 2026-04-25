package com.bizcord.backend.repository;

import com.bizcord.backend.entity.TokenType;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {
    Optional<VerificationToken> findByToken(String token);
    void deleteAllByUserAndType(User user, TokenType type);
}
