package com.laberit.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationCleanupScheduler {

    private final NotificationService notificationService;

    public NotificationCleanupScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    public void cleanupExpiredNotifications() {
        notificationService.cleanupExpiredNotifications();
    }
}
