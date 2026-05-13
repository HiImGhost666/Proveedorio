package com.laberit.backend.controller;

import com.laberit.backend.dtos.supplier.SupplierDashboardDto;
import com.laberit.backend.dtos.supplier.SupplierRequestDto;
import com.laberit.backend.dtos.supplier.SupplierResponseDto;
import com.laberit.backend.dtos.supplier.SupplierSimpleDto;
import com.laberit.backend.service.SupplierService;
import com.laberit.backend.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de proveedores (suppliers).
 * Expone listado simple para clientes y admins, dashboard por proveedor, y CRUD completo solo para admins.
 * Las rutas se mantienen bajo /api/v1/proveedores por compatibilidad con el frontend.
 */
@RestController
@RequestMapping("/api/v1/proveedores")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * Devuelve una lista reducida de proveedores (id y nombre) para desplegables y selección.
     * Accesible por ADMIN y CLIENTE.
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<ApiResponse<List<SupplierSimpleDto>>> getAllSuppliersSimple() {
        List<SupplierSimpleDto> list = supplierService.getAllSuppliersSimple();
        return ResponseEntity.ok(ApiResponse.success(list, "Lista simple de proveedores recuperada"));
    }

    /**
     * Devuelve las métricas del dashboard de un proveedor. Accesible por ADMIN y CLIENTE.
     */
    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<ApiResponse<SupplierDashboardDto>> getSupplierDashboard(@PathVariable Integer id) {
        SupplierDashboardDto dashboardData = supplierService.getDashboardData(id);
        return ResponseEntity.ok(ApiResponse.success(dashboardData, "Datos del dashboard recuperados"));
    }

    /**
     * Lista todos los proveedores con todos sus campos. Solo ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SupplierResponseDto>>> getAllSuppliers() {
        List<SupplierResponseDto> list = supplierService.getAllSuppliers();
        return ResponseEntity.ok(ApiResponse.success(list, "Lista de proveedores recuperada"));
    }

    /**
     * Obtiene un proveedor por su identificador. Solo ADMIN.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> getSupplierById(@PathVariable Integer id) {
        SupplierResponseDto dto = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "Proveedor encontrado"));
    }

    /**
     * Crea un nuevo proveedor. Solo ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> createSupplier(@Valid @RequestBody SupplierRequestDto request) {
        SupplierResponseDto created = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Proveedor creado exitosamente"));
    }

    /**
     * Actualiza un proveedor existente. Solo ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> updateSupplier(
            @PathVariable Integer id,
            @Valid @RequestBody SupplierRequestDto request) {
        SupplierResponseDto updated = supplierService.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Proveedor actualizado"));
    }

    /**
     * Elimina un proveedor por identificador. Solo ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Integer id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
