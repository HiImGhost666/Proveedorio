package com.laberit.backend.service;

import com.laberit.backend.dtos.notification.NotificationCountsDto;
import com.laberit.backend.dtos.notification.NotificationItemDto;
import com.laberit.backend.dtos.notification.NotificationTab;
import com.laberit.backend.entity.*;
import com.laberit.backend.repository.AppNotificationRepository;
import com.laberit.backend.repository.UserNotificationStatusRepository;
import com.laberit.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_LIMIT = 50;
    private static final int DEDUP_WINDOW_MINUTES = 30;

    private final AppNotificationRepository appNotificationRepository;
    private final UserNotificationStatusRepository userNotificationStatusRepository;
    private final UserRepository userRepository;

    public NotificationService(AppNotificationRepository appNotificationRepository,
                               UserNotificationStatusRepository userNotificationStatusRepository,
                               UserRepository userRepository) {
        this.appNotificationRepository = appNotificationRepository;
        this.userNotificationStatusRepository = userNotificationStatusRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createErrorNotification(Supplier supplier, String context, String message) {
        String safeContext = context != null ? context : "Proveedor";
        String safeMessage = message != null ? message : "Error inesperado de integración";
        String title = "Error de proveedor";
        String body = String.format("%s: %s", safeContext, safeMessage);
        String dedupKey = dedupKey("ERROR", supplier != null ? supplier.getId() : null, null, safeContext + "|" + safeMessage);
        createNotification(AppNotificationType.ERROR, title, body, supplier, null, dedupKey, null, null);
    }

    @Transactional
    public void createOfferNotification(MasterProduct product, Supplier supplier, BigDecimal previousPrice, BigDecimal currentPrice) {
        if (product == null || supplier == null || previousPrice == null || currentPrice == null) {
            return;
        }
        if (currentPrice.compareTo(previousPrice) >= 0) {
            return;
        }

        String title = "Bajada de precio";
        String message = String.format(
                "%s %s en %s bajó de %s a %s",
                safe(product.getBrand()),
                safe(product.getModel()),
                safe(supplier.getName()),
                previousPrice,
                currentPrice
        );
        String dedupKey = dedupKey("OFERTA", supplier.getId(), product.getId(), previousPrice + "->" + currentPrice);
        createNotification(AppNotificationType.OFERTA, title, message, supplier, product, dedupKey, productActionPath(product), null);
    }

    @Transactional
    public void createNewProductGlobalNotification(MasterProduct product, Supplier supplier) {
        if (product == null) {
            return;
        }

        String title = "Nuevo producto global";
        String message = String.format(
                "%s %s (%s) se ha incorporado por primera vez",
                safe(product.getBrand()),
                safe(product.getModel()),
                safe(product.getMpn())
        );
        String dedupKey = dedupKey("NUEVO_GLOBAL", supplier != null ? supplier.getId() : null, product.getId(), safe(product.getMpn()));
        createNotification(AppNotificationType.NUEVO_PRODUCTO, title, message, supplier, product, dedupKey, productActionPath(product), "GLOBAL");
    }

    @Transactional
    public void createNewProductSupplierNotification(MasterProduct product, Supplier supplier) {
        if (product == null || supplier == null) {
            return;
        }

        String title = "Nuevo producto en proveedor";
        String message = String.format(
                "%s %s ahora está disponible en %s",
                safe(product.getBrand()),
                safe(product.getModel()),
                safe(supplier.getName())
        );
        String dedupKey = dedupKey("NUEVO_SUPPLIER", supplier.getId(), product.getId(), safe(product.getMpn()));
        createNotification(AppNotificationType.NUEVO_PRODUCTO, title, message, supplier, product, dedupKey, productActionPath(product), "SUPPLIER");
    }

    @Transactional
    public void createGeneralNotification(String title, String message, String dedupBase) {
        String dedupKey = dedupKey("GENERAL", null, null, dedupBase != null ? dedupBase : title + "|" + message);
        createNotification(AppNotificationType.GENERAL, title, message, null, null, dedupKey, null, null);
    }

    @Transactional
    public List<NotificationItemDto> getNotifications(NotificationTab tab, int limit) {
        User currentUser = getCurrentUser();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        AppNotificationType typeFilter = mapTabToType(tab);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        Pageable pageable = PageRequest.of(0, safeLimit);

        List<AppNotification> notifications = appNotificationRepository.findActiveByType(typeFilter, nowUtc, pageable);
        List<Long> ids = notifications.stream().map(AppNotification::getId).toList();

        Set<Long> readIds = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(userNotificationStatusRepository.findReadNotificationIds(currentUser.getId(), ids));

        return notifications.stream()
                .map(n -> toDto(n, readIds.contains(n.getId())))
                .toList();
    }

    @Transactional
    public NotificationCountsDto getUnreadCounts() {
        User currentUser = getCurrentUser();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        List<Long> allActiveIds = appNotificationRepository.findAllActiveIdsByType(null, nowUtc);
        if (allActiveIds.isEmpty()) {
            return new NotificationCountsDto(0, 0, 0, 0);
        }

        Set<Long> readIds = new HashSet<>(userNotificationStatusRepository.findAllReadActiveIds(currentUser.getId(), nowUtc));

        long errores = 0;
        long ofertas = 0;
        long nuevoProducto = 0;
        long general = 0;

        Map<Long, AppNotificationType> typeById = appNotificationRepository.findAllById(allActiveIds).stream()
                .collect(Collectors.toMap(AppNotification::getId, AppNotification::getType));

        for (Long id : allActiveIds) {
            if (readIds.contains(id)) {
                continue;
            }
            AppNotificationType type = typeById.get(id);
            if (type == null) {
                continue;
            }
            general++;
            switch (type) {
                case ERROR -> errores++;
                case OFERTA -> ofertas++;
                case NUEVO_PRODUCTO -> nuevoProducto++;
                case GENERAL -> {
                }
            }
        }
        return new NotificationCountsDto(general, errores, ofertas, nuevoProducto);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        User currentUser = getCurrentUser();

        AppNotification notification = appNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notificación no encontrada"));

        userNotificationStatusRepository.findByUserIdAndNotificationId(currentUser.getId(), notificationId)
                .orElseGet(() -> userNotificationStatusRepository.save(new UserNotificationStatus(notification, currentUser)));
    }

    @Transactional
    public void markAllAsRead(NotificationTab tab) {
        User currentUser = getCurrentUser();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        AppNotificationType typeFilter = mapTabToType(tab);

        List<Long> activeIds = appNotificationRepository.findAllActiveIdsByType(typeFilter, nowUtc);
        if (activeIds.isEmpty()) {
            return;
        }

        Set<Long> alreadyRead = new HashSet<>(userNotificationStatusRepository.findReadNotificationIds(currentUser.getId(), activeIds));
        List<AppNotification> unreadNotifications = appNotificationRepository.findAllById(activeIds).stream()
                .filter(n -> !alreadyRead.contains(n.getId()))
                .toList();

        if (unreadNotifications.isEmpty()) {
            return;
        }

        List<UserNotificationStatus> toSave = unreadNotifications.stream()
                .map(n -> new UserNotificationStatus(n, currentUser))
                .toList();
        userNotificationStatusRepository.saveAll(toSave);
    }

    @Transactional
    public void cleanupExpiredNotifications() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        List<Long> expiredIds = appNotificationRepository.findExpiredIds(nowUtc);
        if (expiredIds.isEmpty()) {
            return;
        }

        userNotificationStatusRepository.deleteByNotificationIds(expiredIds);
        appNotificationRepository.deleteAllByIdInBatch(expiredIds);
        logger.info("Limpieza de notificaciones completada. Eliminadas: {}", expiredIds.size());
    }

    private void createNotification(AppNotificationType type,
                                    String title,
                                    String message,
                                    Supplier supplier,
                                    MasterProduct product,
                                    String dedupKey,
                                    String actionPath,
                                    String scopeTag) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (dedupKey != null && !dedupKey.isBlank()) {
            Optional<AppNotification> existing = appNotificationRepository
                    .findFirstByDedupKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            dedupKey,
                            nowUtc.minusMinutes(DEDUP_WINDOW_MINUTES)
                    );
            if (existing.isPresent()) {
                return;
            }
        }

        AppNotification notification = new AppNotification();
        notification.setType(type);
        notification.setTitle(safe(title));
        notification.setMessage(safe(message));
        notification.setSupplier(supplier);
        notification.setMasterProduct(product);
        notification.setDedupKey(dedupKey);
        notification.setActionPath(actionPath);
        notification.setScopeTag(scopeTag);
        notification.setCreatedAt(nowUtc);
        notification.setExpiresAt(nowUtc.plusHours(24));
        appNotificationRepository.save(notification);
    }

    private NotificationItemDto toDto(AppNotification notification, boolean read) {
        Integer productId = notification.getMasterProduct() != null ? notification.getMasterProduct().getId() : null;
        String productLabel = notification.getMasterProduct() != null
                ? (safe(notification.getMasterProduct().getBrand()) + " " + safe(notification.getMasterProduct().getModel())).trim()
                : null;

        Integer supplierId = notification.getSupplier() != null ? notification.getSupplier().getId() : null;
        String supplierName = notification.getSupplier() != null ? notification.getSupplier().getName() : null;

        return new NotificationItemDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                supplierId,
                supplierName,
                productId,
                productLabel,
                notification.getActionPath(),
                notification.getScopeTag(),
                notification.getCreatedAt(),
                read
        );
    }

    private AppNotificationType mapTabToType(NotificationTab tab) {
        return switch (tab) {
            case GENERAL -> null;
            case ERRORES -> AppNotificationType.ERROR;
            case OFERTAS -> AppNotificationType.OFERTA;
            case NUEVO_PRODUCTO -> AppNotificationType.NUEVO_PRODUCTO;
        };
    }

    private String dedupKey(String category, Integer supplierId, Integer productId, String baseMessage) {
        String normalizedMessage = normalize(baseMessage);
        return String.format("%s|S:%s|P:%s|%s",
                category,
                supplierId != null ? supplierId : "-",
                productId != null ? productId : "-",
                normalizedMessage);
    }

    private String normalize(String text) {
        if (text == null) {
            return "-";
        }
        return text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String productActionPath(MasterProduct product) {
        if (product == null || product.getId() == null) {
            return null;
        }
        return "/product/" + product.getId();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No se pudo identificar el usuario autenticado");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));
    }
}
