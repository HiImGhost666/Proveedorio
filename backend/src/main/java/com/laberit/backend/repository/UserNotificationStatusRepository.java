package com.laberit.backend.repository;

import com.laberit.backend.entity.UserNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserNotificationStatusRepository extends JpaRepository<UserNotificationStatus, Long> {

    Optional<UserNotificationStatus> findByUserIdAndNotificationId(Integer userId, Long notificationId);

    @Query("SELECT s.notification.id FROM UserNotificationStatus s " +
            "WHERE s.user.id = :userId " +
            "AND s.notification.id IN :notificationIds")
    List<Long> findReadNotificationIds(
            @Param("userId") Integer userId,
            @Param("notificationIds") Collection<Long> notificationIds
    );

    @Query("SELECT s.notification.id FROM UserNotificationStatus s " +
            "WHERE s.user.id = :userId " +
            "AND s.notification.expiresAt > :now")
    List<Long> findAllReadActiveIds(
            @Param("userId") Integer userId,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("DELETE FROM UserNotificationStatus s WHERE s.notification.id IN :notificationIds")
    void deleteByNotificationIds(@Param("notificationIds") Collection<Long> notificationIds);
}
