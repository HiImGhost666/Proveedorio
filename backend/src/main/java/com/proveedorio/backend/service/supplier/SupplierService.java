package com.proveedorio.backend.service;

import com.proveedorio.backend.dtos.supplier.SupplierDashboardDto;
import com.proveedorio.backend.dtos.supplier.SupplierRequestDto;
import com.proveedorio.backend.dtos.supplier.SupplierResponseDto;
import com.proveedorio.backend.dtos.supplier.SupplierSimpleDto;
import com.proveedorio.backend.entity.Supplier;
import com.proveedorio.backend.repository.ProductSupplierRepository;
import com.proveedorio.backend.repository.SupplierRepository;
import com.proveedorio.backend.repository.SyncLogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para proveedores (suppliers).
 * Gestiona el CRUD de proveedores, el dashboard por proveedor (métricas de productos y latencia)
 * y la conversión entre entidad {@link Supplier} y DTOs de respuesta/entrada.
 * Usado por {@link com.proveedorio.backend.controller.SupplierController} y por otros servicios que necesitan
 * resolver un proveedor por ID (p. ej. sincronización, historial de precios).
 */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SyncLogRepository syncLogRepository;
    private final EncryptionService encryptionService;

    public SupplierService(
            SupplierRepository supplierRepository,
            ProductSupplierRepository productSupplierRepository,
            SyncLogRepository syncLogRepository,
            EncryptionService encryptionService) {
        this.supplierRepository = supplierRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.syncLogRepository = syncLogRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Obtiene todos los proveedores mapeados a DTO de respuesta.
     */
    @Transactional(readOnly = true)
    public List<SupplierResponseDto> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene solo los proveedores activos en formato reducido (id y nombre) para listas y desplegables.
     */
    @Transactional(readOnly = true)
    public List<SupplierSimpleDto> getAllSuppliersSimple() {
        return supplierRepository.findByActiveTrue().stream()
                .map(s -> new SupplierSimpleDto(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Busca un proveedor por ID y lo devuelve como DTO. Lanza excepción si no existe.
     */
    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierById(Integer id) {
        return supplierRepository.findById(id)
                .map(this::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));
    }

    /**
     * Obtiene la entidad proveedor por ID. Usado por otros servicios que necesitan la entidad.
     */
    @Transactional(readOnly = true)
    public Supplier getById(Integer id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));
    }

    /**
     * Crea un nuevo proveedor a partir del DTO de entrada. Valida que el nombre no esté duplicado.
     */
    @Transactional
    public SupplierResponseDto createSupplier(SupplierRequestDto requestDto) {
        if (supplierRepository.existsByName(requestDto.name())) {
            throw new IllegalArgumentException("Ya existe un proveedor con el nombre: " + requestDto.name());
        }
        Supplier supplier = new Supplier();
        fromDtoToEntity(supplier, requestDto);
        Supplier saved = supplierRepository.save(supplier);
        return toResponseDto(saved);
    }

    /**
     * Actualiza un proveedor existente. Carga la entidad con sus mapeos para actualizar en cascada.
     */
    @Transactional
    public SupplierResponseDto updateSupplier(Integer id, SupplierRequestDto requestDto) {
        Supplier supplier = supplierRepository.findByIdWithMappings(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));
        fromDtoToEntity(supplier, requestDto);
        Supplier updated = supplierRepository.save(supplier);
        return toResponseDto(updated);
    }

    /**
     * Elimina un proveedor por identificador.
     */
    @Transactional
    public void deleteSupplier(Integer id) {
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("Proveedor no encontrado con ID: " + id);
        }
        supplierRepository.deleteById(id);
    }

    /**
     * Calcula las métricas del dashboard para un proveedor: total productos, con/sin stock, latencia media.
     */
    @Transactional(readOnly = true)
    public SupplierDashboardDto getDashboardData(Integer id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));

        long totalProducts = productSupplierRepository.countBySupplierId(id);
        long productsOutOfStock = productSupplierRepository.countBySupplierIdAndStockIsZeroOrNull(id);
        long productsInStock = totalProducts - productsOutOfStock;

        Double avgLatencyMs = syncLogRepository.findAverageLatencyBySupplierId(id);
        double avgLatencyHours = (avgLatencyMs != null ? avgLatencyMs : 0.0) / 1000.0 / 3600.0;

        return new SupplierDashboardDto(
                supplier.getId(),
                supplier.getName(),
                totalProducts,
                productsInStock,
                productsOutOfStock,
                avgLatencyHours
        );
    }

    private void fromDtoToEntity(Supplier supplier, SupplierRequestDto dto) {
        supplier.setName(dto.name());
        supplier.setBaseUrlApi(dto.baseUrlApi());
        supplier.setContact(dto.contact());
        supplier.setEmail(dto.email());
        supplier.setSchedule(dto.schedule());
        supplier.setPhone(dto.phone());
        supplier.setWebsite(dto.website());
        supplier.setCountry(dto.country());
        supplier.setDefaultCurrency(dto.defaultCurrency());
        supplier.setActive(dto.active());
        supplier.setSupportsBulkSync(dto.supportsBulkSync());
        supplier.setCatalogEndpoint(dto.catalogEndpoint());
        supplier.setDetailEndpoint(dto.detailEndpoint());
        supplier.setSearchEndpoint(dto.searchEndpoint());

        if (dto.apiKey() != null && !dto.apiKey().isBlank()) {
            supplier.setApiKey(encryptionService.encrypt(dto.apiKey()));
        } else {
            supplier.setApiKey(encryptionService.encrypt("NO_API_KEY"));
        }
    }

    private SupplierResponseDto toResponseDto(Supplier supplier) {
        return new SupplierResponseDto(
                supplier.getId(),
                supplier.getName(),
                supplier.getBaseUrlApi(),
                supplier.getContact(),
                supplier.getEmail(),
                supplier.getSchedule(),
                supplier.getPhone(),
                supplier.getWebsite(),
                supplier.getCountry(),
                supplier.getDefaultCurrency(),
                Boolean.TRUE.equals(supplier.getActive()),
                Boolean.TRUE.equals(supplier.getSupportsBulkSync()),
                supplier.getCatalogEndpoint(),
                supplier.getDetailEndpoint(),
                supplier.getSearchEndpoint()
        );
    }
}

