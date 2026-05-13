package com.laberit.backend.dtos.supplier;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para crear o actualizar una regla de mapeo de proveedor.
 * Se valida que los campos clave no vayan vacíos para evitar reglas incompletas.
 */
public record SupplierMappingRequestDto(
    @NotBlank(message = "El nombre del campo interno es obligatorio") String internalField,
    @NotBlank(message = "La ruta del campo externo es obligatoria") String externalField,
    @NotBlank(message = "El tipo de transformación es obligatorio") String transformationType
) {}
