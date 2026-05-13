package com.proveedorio.backend.service;

import com.proveedorio.backend.dtos.pricing.PriceHistoryItemDto;
import com.proveedorio.backend.entity.MasterProduct;
import com.proveedorio.backend.entity.PriceHistory;
import com.proveedorio.backend.entity.Supplier;
import com.proveedorio.backend.repository.MasterProductRepository;
import com.proveedorio.backend.repository.PriceHistoryRepository;
import com.proveedorio.backend.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de historial de precios: registro de cambios y consultas por producto y proveedor.
 * Usado por {@link ProductSupplierService}, {@link PriceHistoryController} y {@link ProductController}.
 */
@Service
public class PriceHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PriceHistoryService.class);
    private final PriceHistoryRepository priceHistoryRepository;
    private final MasterProductRepository masterProductRepository;
    private final SupplierRepository supplierRepository;
    private final NotificationService notificationService;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository,
                               MasterProductRepository masterProductRepository,
                               SupplierRepository supplierRepository,
                               NotificationService notificationService) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.masterProductRepository = masterProductRepository;
        this.supplierRepository = supplierRepository;
        this.notificationService = notificationService;
    }

    /**
     * Registra un cambio de precio en el historial si el nuevo precio es distinto al último registrado.
     */
    @Transactional
    public void registerPriceChange(MasterProduct masterProduct, Supplier supplier, BigDecimal newPrice) {
        Optional<PriceHistory> last = priceHistoryRepository
                .findFirstByMasterProductAndSupplierOrderByRegisteredAtDesc(masterProduct, supplier);
        boolean changed = last.map(h -> h.getCostPrice() != null && h.getCostPrice().compareTo(newPrice) != 0).orElse(true);
        if (changed) {
            PriceHistory entry = new PriceHistory(masterProduct, supplier, newPrice);
            BigDecimal previous = last.map(PriceHistory::getCostPrice).orElse(BigDecimal.ZERO);
            String trend = entry.computeTrend(previous);
            logger.info("Cambio de precio detectado para {} ({}): {} -> {} ({})",
                    masterProduct.getModel(), supplier.getName(), previous, newPrice, trend);
            priceHistoryRepository.save(entry);

            if (previous.compareTo(BigDecimal.ZERO) > 0 && newPrice.compareTo(previous) < 0) {
                notificationService.createOfferNotification(masterProduct, supplier, previous, newPrice);
            }
        }
    }

    /**
     * Obtiene el historial de precios para un producto y opcionalmente un proveedor.
     * Si supplier es null, devuelve el historial de todos los proveedores del producto.
     */
    public List<PriceHistory> getHistoryForProduct(MasterProduct masterProduct, Supplier supplier) {
        return priceHistoryRepository.findByMasterProductAndSupplierOrderByRegisteredAtDesc(masterProduct, supplier);
    }

    /**
     * Construye la lista de DTOs de historial para un producto y proveedor con tendencia calculada.
     */
    public List<PriceHistoryItemDto> getHistoryDto(Integer productId, Integer supplierId) {
        logger.debug("Consultando historial para productoId={} y supplierId={}", productId, supplierId);
        MasterProduct masterProduct = masterProductRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado"));

        List<PriceHistory> history = priceHistoryRepository.findByMasterProductAndSupplierOrderByRegisteredAtDesc(masterProduct, supplier);
        List<PriceHistory> chronological = new ArrayList<>(history);
        Collections.reverse(chronological);

        List<PriceHistoryItemDto> dtos = new ArrayList<>();
        BigDecimal previousPrice = BigDecimal.ZERO;
        for (PriceHistory h : chronological) {
            String trend = h.computeTrend(previousPrice);
            dtos.add(new PriceHistoryItemDto(h.getCostPrice(), h.getRegisteredAt(), trend));
            previousPrice = h.getCostPrice();
        }
        Collections.reverse(dtos);
        return dtos;
    }
}

