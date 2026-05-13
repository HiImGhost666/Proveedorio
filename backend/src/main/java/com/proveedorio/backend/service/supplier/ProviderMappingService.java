package com.proveedorio.backend.service;

import com.proveedorio.backend.dtos.supplier.SupplierMappingDto;
import com.proveedorio.backend.dtos.supplier.SupplierMappingRequestDto;
import com.proveedorio.backend.entity.ProviderMapping;
import com.proveedorio.backend.entity.Supplier;
import com.proveedorio.backend.repository.ProviderMappingRepository;
import com.proveedorio.backend.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProviderMappingService {

    private final ProviderMappingRepository mappingRepository;
    private final SupplierRepository supplierRepository;

    public ProviderMappingService(ProviderMappingRepository mappingRepository, SupplierRepository supplierRepository) {
        this.mappingRepository = mappingRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierMappingDto> getMappingsForProvider(Integer supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new EntityNotFoundException("Proveedor no encontrado con ID: " + supplierId);
        }
        return mappingRepository.findBySupplier_Id(supplierId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierMappingDto createMapping(Integer supplierId, SupplierMappingRequestDto requestDto) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + supplierId));

        if (mappingRepository.findBySupplier_IdAndInternalField(supplierId, requestDto.internalField()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un mapeo para el campo interno: " + requestDto.internalField() + " en este proveedor.");
        }

        ProviderMapping mapping = new ProviderMapping();
        mapping.setSupplier(supplier);
        mapping.setInternalField(requestDto.internalField());
        mapping.setExternalField(requestDto.externalField());
        mapping.setTransformationType(requestDto.transformationType());

        ProviderMapping savedMapping = mappingRepository.save(mapping);
        return toDto(savedMapping);
    }

    @Transactional
    public SupplierMappingDto updateMapping(Integer mappingId, SupplierMappingRequestDto requestDto) {
        ProviderMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new EntityNotFoundException("Mapping no encontrado con ID: " + mappingId));

        mappingRepository.findBySupplier_IdAndInternalField(mapping.getSupplier().getId(), requestDto.internalField())
                .ifPresent(existingMapping -> {
                    if (!existingMapping.getMappingId().equals(mappingId)) {
                        throw new IllegalArgumentException("Ya existe un mapeo para el campo interno: " + requestDto.internalField() + " en este proveedor.");
                    }
                });

        mapping.setInternalField(requestDto.internalField());
        mapping.setExternalField(requestDto.externalField());
        mapping.setTransformationType(requestDto.transformationType());

        ProviderMapping updatedMapping = mappingRepository.save(mapping);
        return toDto(updatedMapping);
    }

    @Transactional
    public void deleteMapping(Integer mappingId) {
        if (!mappingRepository.existsById(mappingId)) {
            throw new EntityNotFoundException("Mapping no encontrado con ID: " + mappingId);
        }
        mappingRepository.deleteById(mappingId);
    }


    private SupplierMappingDto toDto(ProviderMapping mapping) {
        return new SupplierMappingDto(
                mapping.getMappingId(),
                mapping.getSupplier().getId(),
                mapping.getInternalField(),
                mapping.getExternalField(),
                mapping.getTransformationType()
        );
    }
}

