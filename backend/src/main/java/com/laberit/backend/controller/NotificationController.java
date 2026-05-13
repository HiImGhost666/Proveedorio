package com.laberit.backend.controller;

import com.laberit.backend.dtos.notification.NotificationCountsDto;
import com.laberit.backend.dtos.notification.NotificationItemDto;
import com.laberit.backend.dtos.notification.NotificationTab;
import com.laberit.backend.service.NotificationService;
import com.laberit.backend.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationItemDto>>> getNotifications(
            @RequestParam(defaultValue = "general") String tab,
            @RequestParam(defaultValue = "50") int limit
    ) {
        NotificationTab parsedTab = NotificationTab.fromQuery(tab);
        List<NotificationItemDto> notifications = notificationService.getNotifications(parsedTab, limit);
        return ResponseEntity.ok(ApiResponse.success(notifications, "Notificaciones obtenidas"));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<NotificationCountsDto>> getCounts() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCounts(), "Conteos de notificaciones obtenidos"));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificación marcada como leída"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@RequestParam(defaultValue = "general") String tab) {
        NotificationTab parsedTab = NotificationTab.fromQuery(tab);
        notificationService.markAllAsRead(parsedTab);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificaciones marcadas como leídas"));
    }
}
