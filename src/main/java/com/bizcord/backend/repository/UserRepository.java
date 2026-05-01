package com.bizcord.backend.repository;

import com.bizcord.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("""
            select u from bizcord_users u
            where u.deleted = false
              and u.id <> :currentUserId
              and (lower(u.username) like lower(concat('%', :query, '%'))
                   or lower(u.fullName) like lower(concat('%', :query, '%')))
            order by u.username asc
            """)
    List<User> searchUsers(String query, String currentUserId, Pageable pageable);
}
