package com.laberit.backend.exception;

import com.laberit.backend.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación de @Valid en DTOs de entrada.
     * Construye un payload con mensaje genérico y mapa de errores por campo.
     *
     * Ejemplo de respuesta JSON:
     * {
     *   "success": false,
     *   "message": "Error de validación en la solicitud",
     *   "errors": {
     *     "username": "El nombre de usuario es obligatorio",
     *     "email": "El email no es válido"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiResponse<Void> response = ApiResponse.error("Error de validación en la solicitud");
        response.setErrors(errors);
        response.setMetadata(Map.of("path", request.getDescription(false).replace("uri=", "")));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        // Usamos el método estático 'error' y luego añadimos metadatos
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        response.setMetadata(Map.of("path", request.getDescription(false).replace("uri=", "")));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        response.setMetadata(Map.of("path", request.getDescription(false).replace("uri=", "")));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        ApiResponse<Void> response = ApiResponse.error("Error interno del servidor: " + ex.getMessage());
        response.setMetadata(Map.of("path", request.getDescription(false).replace("uri=", "")));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
