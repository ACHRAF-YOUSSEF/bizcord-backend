package com.bizcord.backend.repository;

import com.bizcord.backend.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    @Query("""
            select n from bizcord_notifications n
            where n.user.id = :userId
            order by n.read asc, n.createdAt desc
            """)
    List<Notification> findAllByUserIdOrdered(String userId, Pageable pageable);

    @Query("""
            select n from bizcord_notifications n
            where n.user.id = :userId and n.createdAt < :cursor
            order by n.read asc, n.createdAt desc
            """)
    List<Notification> findAllByUserIdBeforeCursor(String userId, LocalDateTime cursor, Pageable pageable);

    @Query("select count(n) from bizcord_notifications n where n.user.id = :userId and n.read = false")
    long countUnreadByUserId(String userId);

    @Modifying
    @Query("update bizcord_notifications n set n.read = true where n.user.id = :userId and n.read = false")
    void markAllReadByUserId(String userId);
}
