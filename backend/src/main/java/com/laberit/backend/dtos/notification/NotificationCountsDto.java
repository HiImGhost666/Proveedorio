package com.laberit.backend.dtos.notification;

public record NotificationCountsDto(
        long general,
        long errores,
        long ofertas,
        long nuevoProducto
) {
}
