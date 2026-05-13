package com.proveedorio.backend.controller;

import com.proveedorio.backend.dtos.supplier.SupplierMappingDto;
import com.proveedorio.backend.dtos.supplier.SupplierMappingRequestDto;
import com.proveedorio.backend.service.ProviderMappingService;
import com.proveedorio.backend.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de mapeos de proveedor (campo interno ↔ externo para normalización de APIs).
 * Rutas: /api/v1/proveedores/{id}/mappings y /api/v1/mappings/{id}. Mensajes en español.
 */
@RestController
@RequestMapping("/api/v1")
public class ProviderMappingController {

    private final ProviderMappingService mappingService;

    public ProviderMappingController(ProviderMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping("/proveedores/{proveedorId}/mappings")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<SupplierMappingDto>>> getMappingsForProvider(@PathVariable Integer proveedorId) {
        List<SupplierMappingDto> mappings = mappingService.getMappingsForProvider(proveedorId);
        return ResponseEntity.ok(ApiResponse.success(mappings, "Mapeos del proveedor recuperados"));
    }

    @PostMapping("/proveedores/{proveedorId}/mappings")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SupplierMappingDto>> createMapping(@PathVariable Integer proveedorId, @RequestBody SupplierMappingRequestDto requestDto) {
        SupplierMappingDto newMapping = mappingService.createMapping(proveedorId, requestDto);
        return new ResponseEntity<>(ApiResponse.success(newMapping, "Mapeo creado correctamente."), HttpStatus.CREATED);
    }

    @PutMapping("/mappings/{mappingId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SupplierMappingDto>> updateMapping(@PathVariable Integer mappingId, @RequestBody SupplierMappingRequestDto requestDto) {
        SupplierMappingDto updatedMapping = mappingService.updateMapping(mappingId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(updatedMapping, "Mapeo actualizado correctamente."));
    }

    @DeleteMapping("/mappings/{mappingId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMapping(@PathVariable Integer mappingId) {
        mappingService.deleteMapping(mappingId);
        return ResponseEntity.ok(ApiResponse.success(null, "Mapeo eliminado correctamente."));
    }
}
