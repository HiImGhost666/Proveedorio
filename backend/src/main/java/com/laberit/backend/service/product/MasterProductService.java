package com.laberit.backend.service;

import com.laberit.backend.entity.MasterProduct;
import com.laberit.backend.repository.MasterProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Servicio de solo lectura para obtener la entidad producto maestro por identificador.
 * Usado principalmente por {@link com.laberit.backend.controller.PriceHistoryController}
 * para resolver el producto antes de consultar historial de precios y análisis.
 */
@Service
public class MasterProductService {

    private final MasterProductRepository masterProductRepository;

    public MasterProductService(MasterProductRepository masterProductRepository) {
        this.masterProductRepository = masterProductRepository;
    }

    /**
     * Obtiene un producto maestro por su identificador interno.
     *
     * @param id identificador de base de datos del producto maestro.
     * @return la entidad {@link MasterProduct} encontrada.
     * @throws jakarta.persistence.EntityNotFoundException si no existe producto con ese id.
     */
    public MasterProduct getById(Integer id) {
        return masterProductRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
    }
}

