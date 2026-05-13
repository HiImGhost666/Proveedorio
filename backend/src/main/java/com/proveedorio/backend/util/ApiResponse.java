package com.proveedorio.backend.util;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ApiResponse<T> {
    private Boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    /**
     * Mapa opcional de errores de validación por campo (clave = nombre del campo).
     * Solo se rellena en respuestas de error de validación (400).
     */
    private Map<String, String> errors;

    // Constructor que inicializa los valores por defecto
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
        this.errors = new HashMap<>();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    // Getters y Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Map<String, String> getErrors() { return errors; }
    public void setErrors(Map<String, String> errors) { this.errors = errors; }
}
