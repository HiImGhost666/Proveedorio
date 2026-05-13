package com.proveedorio.backend.repository;

import com.proveedorio.backend.entity.AppNotification;
import com.proveedorio.backend.entity.AppNotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    @Query("SELECT n FROM AppNotification n " +
            "WHERE n.expiresAt > :now " +
            "AND (:type IS NULL OR n.type = :type) " +
            "ORDER BY n.createdAt DESC")
    List<AppNotification> findActiveByType(
            @Param("type") AppNotificationType type,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("SELECT n.id FROM AppNotification n " +
            "WHERE n.expiresAt > :now " +
            "AND (:type IS NULL OR n.type = :type)")
    List<Long> findAllActiveIdsByType(
            @Param("type") AppNotificationType type,
            @Param("now") LocalDateTime now
    );

    Optional<AppNotification> findFirstByDedupKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String dedupKey,
            LocalDateTime since
    );

    @Query("SELECT n.id FROM AppNotification n WHERE n.expiresAt <= :now")
    List<Long> findExpiredIds(@Param("now") LocalDateTime now);
}
