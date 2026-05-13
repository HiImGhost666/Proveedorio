package com.proveedorio.backend.dtos.notification;

public enum NotificationTab {
    GENERAL,
    ERRORES,
    OFERTAS,
    NUEVO_PRODUCTO;

    public static NotificationTab fromQuery(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return GENERAL;
        }

        String normalized = rawValue.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "GENERAL" -> GENERAL;
            case "ERRORES", "ERROR" -> ERRORES;
            case "OFERTAS", "OFERTA" -> OFERTAS;
            case "NUEVO_PRODUCTO", "NUEVOPRODUCTO", "NUEVO" -> NUEVO_PRODUCTO;
            default -> GENERAL;
        };
    }
}
