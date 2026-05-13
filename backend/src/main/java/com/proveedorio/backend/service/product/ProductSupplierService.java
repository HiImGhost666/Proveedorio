package com.proveedorio.backend.service;

import com.proveedorio.backend.entity.MasterProduct;
import com.proveedorio.backend.entity.PriceHistory;
import com.proveedorio.backend.entity.ProductSupplier;
import com.proveedorio.backend.entity.StockHistory;
import com.proveedorio.backend.entity.Supplier;
import com.proveedorio.backend.repository.ProductSupplierRepository;
import com.proveedorio.backend.repository.StockHistoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Servicio que gestiona la relación producto–proveedor (tabla intermedia product_supplier).
 * Actualiza o crea la oferta de un proveedor para un producto maestro, registra cambios de precio
 * en {@link PriceHistory} y cambios de stock en {@link StockHistory}.
 */
@Service
public class ProductSupplierService {

    private final ProductSupplierRepository productSupplierRepository;
    private final PriceHistoryService priceHistoryService;
    private final NotificationService notificationService;
    private final StockHistoryRepository stockHistoryRepository;

    public ProductSupplierService(
            ProductSupplierRepository productSupplierRepository,
            PriceHistoryService priceHistoryService,
            NotificationService notificationService,
            StockHistoryRepository stockHistoryRepository) {
        this.productSupplierRepository = productSupplierRepository;
        this.priceHistoryService = priceHistoryService;
        this.notificationService = notificationService;
        this.stockHistoryRepository = stockHistoryRepository;
    }

    /**
     * Crea o actualiza la oferta de un proveedor para un producto maestro (upsert).
     * Actualiza precio, stock, PVP, EAN, MOQ y condición; si el precio cambia o es una oferta nueva,
     * registra un registro en {@link PriceHistory} mediante {@link PriceHistoryService#registerPriceChange}.
     *
     * @param masterProduct producto maestro (debe tener ID asignado).
     * @param proveedor     proveedor de la oferta.
     * @param data          datos normalizados del catálogo (precio, stock, etc.).
     * @return la entidad {@link ProductSupplier} guardada.
     * @throws IllegalArgumentException si el producto maestro no tiene ID (mensaje en español).
     */
    @Transactional
    public ProductSupplier upsertProductSupplier(MasterProduct masterProduct, Supplier supplier, UniversalMapperService.NormalizedData data) {

        if (masterProduct.getId() == null) {
            throw new IllegalArgumentException("El producto maestro debe tener un ID para poder crear o actualizar la oferta del proveedor.");
        }

        ProductSupplier ps = productSupplierRepository.findByMasterProductIdAndSupplierId(masterProduct.getId(), supplier.getId())
                .orElseGet(() -> {
                    ProductSupplier newPs = new ProductSupplier();
                    newPs.setMasterProduct(masterProduct);
                    newPs.setSupplier(supplier);
                    return newPs;
                });

        BigDecimal costoApi = data.price();
        Integer stockApi = data.stock();

        final boolean isNewEntry = ps.getId() == null;
        final boolean priceHasChanged = !isNewEntry && ps.getPrice() != null && costoApi != null && ps.getPrice().compareTo(costoApi) != 0;

        if ((isNewEntry && costoApi != null) || priceHasChanged) {
            priceHistoryService.registerPriceChange(masterProduct, supplier, costoApi);
        }

        if (isNewEntry) {
            notificationService.createNewProductSupplierNotification(masterProduct, supplier);
        }

        // Registrar cambio de stock en stock_history solo cuando el valor cambia
        int newStock = stockApi != null ? stockApi : 0;
        int currentStock = ps.getStock() != null ? ps.getStock() : -1;
        if (isNewEntry || currentStock != newStock) {
            stockHistoryRepository.save(new StockHistory(masterProduct, supplier, newStock));
        }

        // Actualizamos los datos básicos
        ps.setExternalProviderId(data.externalId());
        ps.setPrice(costoApi); // Precio Coste
        ps.setStock(newStock);

        // Actualizamos los NUEVOS campos comerciales
        ps.setRetailPrice(data.retailPrice()); // PVP
        ps.setEan(data.ean());
        ps.setMoq(data.moq());
        
        // Mapeo de condición (String -> Enum)
        if (data.condition() != null) {
            try {
                // Intentamos mapear strings comunes como "new", "used", "refurbished"
                String condStr = data.condition().toUpperCase();
                if (condStr.contains("NEW") || condStr.contains("NUEVO")) {
                    ps.setProductCondition(ProductSupplier.ProductCondition.NEW);
                } else if (condStr.contains("REFURB") || condStr.contains("REACOND")) {
                    ps.setProductCondition(ProductSupplier.ProductCondition.REFURBISHED);
                } else if (condStr.contains("USED") || condStr.contains("USADO")) {
                    ps.setProductCondition(ProductSupplier.ProductCondition.USED);
                } else if (condStr.contains("DAMAGED") || condStr.contains("DAÑADO")) {
                    ps.setProductCondition(ProductSupplier.ProductCondition.BOX_DAMAGED);
                }
            } catch (Exception e) {
                // Si falla, dejamos el valor por defecto (NEW) o el que tuviera
            }
        }

        ps.setLastUpdated(LocalDateTime.now());
        ps.setAvailable(newStock > 0);
        ps.markAsFresh();

        return productSupplierRepository.save(ps);
    }

    /**
     * Marca la oferta producto–proveedor como con error de sincronización y guarda el mensaje.
     * Si no existe la fila, crea una con valores por defecto para poder persistir el error.
     *
     * @param masterProduct producto maestro afectado.
     * @param proveedor     proveedor afectado.
     * @param mensajeError  mensaje de error a almacenar (no debe incluir datos sensibles).
     */
    @Transactional
    public void logProviderError(MasterProduct masterProduct, Supplier supplier, String mensajeError) {
        ProductSupplier ps = productSupplierRepository.findByMasterProductIdAndSupplierId(masterProduct.getId(), supplier.getId())
                .orElseGet(() -> {
                    ProductSupplier newPs = new ProductSupplier();
                    newPs.setMasterProduct(masterProduct);
                    newPs.setSupplier(supplier);
                    // Valores dummy para permitir persistencia
                    newPs.setPrice(BigDecimal.ZERO);
                    newPs.setStock(0);
                    newPs.setLastUpdated(LocalDateTime.now());
                    newPs.setAvailable(false);
                    return newPs;
                });

        ps.registerError(mensajeError);
        productSupplierRepository.save(ps);
    }
}

