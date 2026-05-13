package com.laberit.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laberit.backend.dtos.DisponibilidadStatus;
import com.laberit.backend.dtos.SyncStatus;
import com.laberit.backend.dtos.product.AggregatedProductDto;
import com.laberit.backend.dtos.product.ProductDetailDto;
import com.laberit.backend.dtos.product.ProductSearchRequest;
import com.laberit.backend.dtos.product.SupplierOfferDto;
import com.laberit.backend.entity.MasterProduct;
import com.laberit.backend.entity.ProductSupplier;
import com.laberit.backend.entity.Supplier;
import com.laberit.backend.repository.MasterProductRepository;
import com.laberit.backend.repository.ProductSupplierRepository;
import com.laberit.backend.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementación principal del servicio de inventario.
 * Coordina la búsqueda y listado de productos maestros, la sincronización con catálogos de proveedores
 * y la construcción de DTOs para el listado consolidado y el detalle de producto.
 * Utiliza {@link MasterProductRepository}, {@link ProductSupplierRepository}, {@link ApiClientService},
 * {@link UniversalMapperService} y {@link ProductSupplierService} para obtener y persistir datos.
 */
@Service
public class StockServiceImpl implements IStockService {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);
    private static final int MIN_STOCK_THRESHOLD = 5;

    private final MasterProductRepository masterProductRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SupplierRepository supplierRepository;
    private final ApiClientService apiClientService;
    private final UniversalMapperService universalMapperService;
    private final ProductSupplierService productSupplierService;
    private final SyncStateService syncStateService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public StockServiceImpl(MasterProductRepository masterProductRepository,
                            ProductSupplierRepository productSupplierRepository,
                            SupplierRepository supplierRepository,
                            ApiClientService apiClientService,
                            UniversalMapperService universalMapperService,
                            ProductSupplierService productSupplierService,
                            SyncStateService syncStateService,
                            NotificationService notificationService,
                            ObjectMapper objectMapper) {
        this.masterProductRepository = masterProductRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.supplierRepository = supplierRepository;
        this.apiClientService = apiClientService;
        this.universalMapperService = universalMapperService;
        this.productSupplierService = productSupplierService;
        this.syncStateService = syncStateService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Busca productos maestros aplicando filtros de texto, categoría, proveedor y especificaciones.
     * Si el orden solicitado es por disponibilidad o precio mínimo, se cargan todos los resultados
     * en memoria para ordenar y luego se aplica la paginación; en caso contrario se delega la
     * paginación al repositorio.
     *
     * @param request  filtros de búsqueda (query, categoria, supplierId, specsFilter, etc.).
     * @param pageable tamaño de página e índice.
     * @return página de {@link AggregatedProductDto} con los productos que cumplen los filtros.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AggregatedProductDto> searchProducts(ProductSearchRequest request, Pageable pageable) {
        logger.info("Buscando productos con filtros: query='{}', brandFilter='{}', productCategory='{}', supplierId={}, specsFilter='{}'",
                request.query(), request.category(), request.productCategory(), request.supplierId(), request.specsFilter());

        Sort sort = pageable.getSort();
        boolean customSort = false;
        Sort.Direction direction = Sort.Direction.ASC;
        String sortProperty = "";

        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                if ("disponibilidad".equals(order.getProperty()) ||
                        "disponibilidadRank".equals(order.getProperty()) ||
                        "precioMinimo".equals(order.getProperty()) ||
                        "minCostPrice".equals(order.getProperty())) {
                    customSort = true;
                    direction = order.getDirection();
                    sortProperty = order.getProperty();
                    break;
                }
            }
        }

        if (customSort) {
            List<MasterProduct> allProducts = masterProductRepository.searchProductsList(
                    request.query(), request.category(), request.productCategory(), request.supplierId(), request.specsFilter()
            );

            List<AggregatedProductDto> allDtos = allProducts.stream()
                    .map(this::convertToAggregatedDto)
                    .collect(Collectors.toList());

            Comparator<AggregatedProductDto> comparator;
            if ("precioMinimo".equals(sortProperty) || "minCostPrice".equals(sortProperty)) {
                comparator = Comparator.comparing(AggregatedProductDto::minCostPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            } else {
                comparator = Comparator.comparingInt(this::getAvailabilityScore);
            }

            if (direction == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }

            allDtos.sort(comparator);

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allDtos.size());
            List<AggregatedProductDto> pageContent;

            if (start >= allDtos.size()) {
                pageContent = List.of();
            } else {
                pageContent = allDtos.subList(start, end);
            }

            return new PageImpl<>(pageContent, pageable, allDtos.size());

        } else {
            Page<MasterProduct> productosPage = masterProductRepository.searchProducts(
                    request.query(), request.category(), request.productCategory(), request.supplierId(), request.specsFilter(), pageable
            );
            logger.info("Resultados encontrados: {}", productosPage.getTotalElements());
            List<AggregatedProductDto> dtos = productosPage.getContent().stream()
                    .map(this::convertToAggregatedDto)
                    .collect(Collectors.toList());
            return new PageImpl<>(dtos, pageable, productosPage.getTotalElements());
        }
    }

    private int getAvailabilityScore(AggregatedProductDto dto) {
        if (dto.availability() == null) return 0;
        switch (dto.availability()) {
            case DISPONIBLE:
                return 3;
            case BAJO_STOCK:
                return 2;
            case SIN_STOCK:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Devuelve todos los productos maestros en formato de lista agregada, paginados.
     * Usado por el listado principal de inventario cuando no hay filtros.
     *
     * @param pageable tamaño e índice de página.
     * @return página de {@link AggregatedProductDto}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AggregatedProductDto> findAllProducts(Pageable pageable) {
        Page<MasterProduct> productosPage = masterProductRepository.findAll(pageable);
        return productosPage.map(this::convertToAggregatedDto);
    }

    /**
     * Obtiene el detalle de un producto maestro por su ID, incluyendo todas las ofertas por proveedor.
     * Usado por el endpoint de detalle de producto en el frontend.
     *
     * @param id identificador del producto maestro.
     * @return opcional con {@link ProductDetailDto} si existe el producto, vacío si no.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDetailDto> findProductDetailsById(Integer id) {
        return masterProductRepository.findById(id).map(this::convertToDetailDto);
    }

    /**
     * Dispara la sincronización masiva de catálogos de todos los proveedores activos.
     * Se ejecuta de forma asíncrona; solo una réplica del sistema puede ejecutarla a la vez
     * gracias a {@link SyncStateService}. Cada proveedor se procesa en paralelo y se actualizan
     * {@link MasterProduct} y {@link ProductSupplier} mediante {@link #syncProviderCatalog(Supplier)}.
     */
    @Override
    @Async
    public void syncAllProducts() {
        // Atomic lock: only one replica across the cluster will proceed
        if (!syncStateService.tryAcquireLock()) {
            logger.warn("La sincronización ya está en progreso en otra réplica. Se omite esta solicitud.");
            return;
        }
        logger.info("=== INICIANDO SINCRONIZACIÓN TOTAL DE CATÁLOGOS ===");
        try {
            List<Supplier> suppliers = supplierRepository.findAllByActiveTrueWithMappings();
            List<CompletableFuture<Void>> futures = suppliers.stream()
                    .map(supplier -> CompletableFuture.runAsync(() -> syncProviderCatalog(supplier), executorService))
                    .toList();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            logger.info("=== SINCRONIZACIÓN TOTAL DE CATÁLOGOS COMPLETADA ===");
            syncStateService.setStatus(SyncStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("!!! ERROR CRÍTICO durante la sincronización total de catálogos", e);
            syncStateService.setStatus(SyncStatus.ERROR);
        }
    }

    /**
     * Devuelve el estado actual de la sincronización global, delegando en {@link SyncStateService}.
     */
    @Override
    @Transactional(readOnly = true)
    public SyncStatus getSyncStatus() {
        return syncStateService.getStatus();
    }

    /**
     * Sincroniza el catálogo completo de un proveedor concreto:
     * <ul>
     *     <li>Llama a la API externa mediante {@link ApiClientService#fetchCatalog(Supplier)}.</li>
     *     <li>Normaliza la respuesta con {@link UniversalMapperService#normalizeResponse(String, Supplier)}.</li>
     *     <li>Actualiza/crea {@link MasterProduct} y sus ofertas {@link ProductSupplier}.</li>
     * </ul>
     */
    private void syncProviderCatalog(Supplier supplier) {
        try {
            String rawJson = apiClientService.fetchCatalog(supplier);
            List<UniversalMapperService.NormalizedData> normalizedList =
                    universalMapperService.normalizeResponse(rawJson, supplier);

            for (UniversalMapperService.NormalizedData data : normalizedList) {
                if (data.mpn() == null) {
                    continue;
                }

                Optional<MasterProduct> existingMaster = masterProductRepository.findByMpn(data.mpn());
                boolean isNewMaster = existingMaster.isEmpty();

                MasterProduct master = existingMaster.orElseGet(() -> {
                    MasterProduct mp = new MasterProduct();
                    mp.setMpn(data.mpn());
                    mp.setBrand(data.brand());
                    mp.setModel(data.model());
                    mp.setCategory(data.category());
                    mp.setTechSpecs(data.techSpecs());
                    return mp;
                });

                // Si el producto ya existía, actualizamos marca/modelo/categoría si vienen informados
                if (data.brand() != null) {
                    master.setBrand(data.brand());
                }
                if (data.model() != null) {
                    master.setModel(data.model());
                }
                if (data.category() != null && !data.category().isBlank()) {
                    master.setCategory(data.category());
                }

                // Merge sencillo de especificaciones técnicas (JSON) si ambas partes tienen contenido
                if (data.techSpecs() != null && !data.techSpecs().isBlank()) {
                    try {
                        Map<String, String> existingSpecs = master.getTechSpecs() != null && !master.getTechSpecs().isBlank()
                                ? objectMapper.readValue(master.getTechSpecs(), new TypeReference<Map<String, String>>() {})
                                : Map.of();

                        Map<String, String> newSpecs = objectMapper.readValue(data.techSpecs(), new TypeReference<Map<String, String>>() {});

                        // Unimos los mapas, dando prioridad a las nuevas claves
                        Map<String, String> merged = existingSpecs.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        merged.putAll(newSpecs);
                        master.setTechSpecs(objectMapper.writeValueAsString(merged));
                    } catch (JsonProcessingException e) {
                        // Si falla el merge, dejamos las nuevas especificaciones tal cual
                        master.setTechSpecs(data.techSpecs());
                    } catch (IOException e) {
                        master.setTechSpecs(data.techSpecs());
                    }
                }

                // Persistimos el producto maestro y la oferta del proveedor
                MasterProduct savedMaster = masterProductRepository.save(master);
                if (isNewMaster) {
                    notificationService.createNewProductGlobalNotification(savedMaster, supplier);
                }
                productSupplierService.upsertProductSupplier(savedMaster, supplier, data);
            }
        } catch (Exception e) {
            logger.error("Error sincronizando el catálogo del proveedor {}: {}", supplier.getName(), e.getMessage(), e);
        }
    }

    /**
     * Convierte un {@link MasterProduct} a su vista agregada para listados.
     */
    private AggregatedProductDto convertToAggregatedDto(MasterProduct masterProduct) {
        List<ProductSupplier> offers = masterProduct.getProductSuppliers();

        BigDecimal minCost = offers.stream()
                .map(ProductSupplier::getPrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal minRetail = offers.stream()
                .map(ProductSupplier::getRetailPrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);

        int supplierCount = offers.size();

        int totalStock = offers.stream()
                .map(ProductSupplier::getStock)
                .filter(s -> s != null && s > 0)
                .mapToInt(Integer::intValue)
                .sum();

        DisponibilidadStatus availability;
        if (totalStock == 0) {
            availability = DisponibilidadStatus.SIN_STOCK;
        } else if (totalStock <= MIN_STOCK_THRESHOLD) {
            availability = DisponibilidadStatus.BAJO_STOCK;
        } else {
            availability = DisponibilidadStatus.DISPONIBLE;
        }

        Map<String, String> specs = Map.of();
        if (masterProduct.getTechSpecs() != null && !masterProduct.getTechSpecs().isBlank()) {
            try {
                specs = objectMapper.readValue(masterProduct.getTechSpecs(), new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                logger.warn("No se pudieron deserializar las especificaciones de producto {}: {}", masterProduct.getId(), e.getMessage());
            }
        }

        return new AggregatedProductDto(
                masterProduct.getId(),
                masterProduct.getMpn(),
                masterProduct.getModel(),
                masterProduct.getBrand(),
                masterProduct.getCategory(),
                minCost,
                minRetail,
                availability,
                supplierCount,
                specs
        );
    }

    /**
     * Convierte un {@link MasterProduct} a {@link ProductDetailDto}, incluyendo todas las ofertas.
     */
    private ProductDetailDto convertToDetailDto(MasterProduct masterProduct) {
        Map<String, String> specs = Map.of();
        if (masterProduct.getTechSpecs() != null && !masterProduct.getTechSpecs().isBlank()) {
            try {
                specs = objectMapper.readValue(masterProduct.getTechSpecs(), new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                logger.warn("No se pudieron deserializar las especificaciones de producto {}: {}", masterProduct.getId(), e.getMessage());
            }
        }

        List<SupplierOfferDto> offers = masterProduct.getProductSuppliers().stream()
                .map(this::toSupplierOfferDto)
                .collect(Collectors.toList());

        return new ProductDetailDto(
                masterProduct.getId(),
                masterProduct.getMpn(),
                masterProduct.getModel(),
                masterProduct.getBrand(),
                masterProduct.getCategory(),
                specs,
                offers
        );
    }

    /**
     * Construye el DTO de oferta de proveedor a partir de la entidad {@link ProductSupplier}.
     */
    private SupplierOfferDto toSupplierOfferDto(ProductSupplier ps) {
        String condition = ps.getProductCondition() != null ? ps.getProductCondition().name() : null;

        String stockStatus;
        Integer stock = ps.getStock();
        if (stock == null || stock == 0) {
            stockStatus = "SIN_STOCK";
        } else if (stock <= MIN_STOCK_THRESHOLD) {
            stockStatus = "BAJO_STOCK";
        } else {
            stockStatus = "DISPONIBLE";
        }

        return new SupplierOfferDto(
                ps.getSupplier() != null ? ps.getSupplier().getId() : null,
                ps.getSupplier() != null ? ps.getSupplier().getName() : null,
                ps.getPrice(),
                ps.getRetailPrice(),
                ps.getStock(),
                ps.getLastUpdated(),
                stockStatus,
                ps.getEan(),
                ps.getMoq(),
                condition
        );
    }
}

