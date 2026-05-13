package com.proveedorio.backend.repository;

import com.proveedorio.backend.dtos.dashboard.ChartDataDto;
import com.proveedorio.backend.entity.MasterProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link MasterProduct}.
 * Usado por {@link com.proveedorio.backend.service.StockServiceImpl} para búsqueda, paginación y sincronización,
 * y por el dashboard para métricas de inventario.
 */
@Repository
public interface MasterProductRepository extends JpaRepository<MasterProduct, Integer> {

    Optional<MasterProduct> findByMpn(String mpn);

    /**
     * Búsqueda paginada de productos maestros aplicando filtros opcionales.
     * <ul>
     *     <li><b>query</b>: busca por modelo, MPN o marca (LIKE case-insensitive).</li>
     *     <li><b>categoria</b>: filtra por marca exacta.</li>
     *     <li><b>proveedorId</b>: exige al menos una oferta de ese proveedor (subquery EXISTS sobre ProductSupplier).</li>
     *     <li><b>specsFilter</b>: busca texto dentro del JSON de especificaciones técnicas.</li>
     * </ul>
     */
    @Query("SELECT p FROM MasterProduct p WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(p.model) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.mpn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoria IS NULL OR :categoria = '' OR p.brand = :categoria) AND " +
           "(:productCategory IS NULL OR :productCategory = '' OR p.category = :productCategory) AND " +
           "(:proveedorId IS NULL OR EXISTS (SELECT 1 FROM ProductSupplier ps WHERE ps.masterProduct = p AND ps.supplier.id = :proveedorId)) AND " +
           "(:specsFilter IS NULL OR :specsFilter = '' OR LOWER(p.techSpecs) LIKE LOWER(CONCAT('%', :specsFilter, '%')))")
    Page<MasterProduct> searchProducts(
        @Param("query") String query,
        @Param("categoria") String categoria,
        @Param("productCategory") String productCategory,
        @Param("proveedorId") Integer proveedorId,
        @Param("specsFilter") String specsFilter,
        Pageable pageable
    );

    /**
     * Variante sin paginación que devuelve lista completa de productos para ordenaciones
     * personalizadas en memoria (por precio mínimo, disponibilidad, etc.).
     * La lógica de filtros es idéntica a {@link #searchProducts(String, String, Integer, String, Pageable)}.
     */
    @Query("SELECT p FROM MasterProduct p WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(p.model) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.mpn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoria IS NULL OR :categoria = '' OR p.brand = :categoria) AND " +
           "(:productCategory IS NULL OR :productCategory = '' OR p.category = :productCategory) AND " +
           "(:proveedorId IS NULL OR EXISTS (SELECT 1 FROM ProductSupplier ps WHERE ps.masterProduct = p AND ps.supplier.id = :proveedorId)) AND " +
           "(:specsFilter IS NULL OR :specsFilter = '' OR LOWER(p.techSpecs) LIKE LOWER(CONCAT('%', :specsFilter, '%')))")
    List<MasterProduct> searchProductsList(
        @Param("query") String query,
        @Param("categoria") String categoria,
        @Param("productCategory") String productCategory,
        @Param("proveedorId") Integer proveedorId,
        @Param("specsFilter") String specsFilter
    );

    /**
     * Cuenta productos "disponibles" según un umbral mínimo de stock
     * (al menos una oferta con stock >= minStockThreshold).
     */
    @Query("SELECT COUNT(DISTINCT p.id) FROM MasterProduct p JOIN p.productSuppliers ps WHERE ps.stock >= :minStockThreshold")
    long countProductosDisponibles(@Param("minStockThreshold") int minStockThreshold);

    /**
     * Cuenta productos con stock bajo: la suma de stock de todas las ofertas
     * es mayor que 0 pero menor que el umbral configurado.
     */
    @Query("SELECT COUNT(DISTINCT p.id) FROM MasterProduct p JOIN p.productSuppliers ps " +
           "WHERE (SELECT SUM(ps2.stock) FROM ProductSupplier ps2 WHERE ps2.masterProduct = p) > 0 " +
           "AND (SELECT SUM(ps2.stock) FROM ProductSupplier ps2 WHERE ps2.masterProduct = p) < :minStockThreshold")
    long countProductosBajoStock(@Param("minStockThreshold") int minStockThreshold);

    /**
     * Devuelve las marcas con mayor número de productos para el gráfico de "Top marcas".
     */
    @Query("SELECT new com.proveedorio.backend.dtos.dashboard.ChartDataDto(p.brand, COUNT(p)) " +
           "FROM MasterProduct p " +
           "GROUP BY p.brand " +
           "ORDER BY COUNT(p) DESC")
    List<ChartDataDto> findTopBrands(Pageable pageable);

    /**
     * Obtiene productos "zombies": aquellos cuya fecha de actualización es anterior
     * a la fecha de corte indicada (sincronizaciones muy antiguas).
     */
    @Query("SELECT p FROM MasterProduct p WHERE p.fechaActualizacion < :cutoffDate")
    List<MasterProduct> findStaleProducts(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * Tendencia de crecimiento del catalogo: productos nuevos por semana en las ultimas N semanas.
     * Columnas: year, week, new_products
     */
    @Query(value = """
        SELECT
            YEAR(mp.fecha_creacion)          AS year,
            WEEK(mp.fecha_creacion, 1)       AS week,
            COUNT(*)                         AS new_products
        FROM producto_maestro mp
        WHERE mp.fecha_creacion >= DATE_SUB(NOW(), INTERVAL :weeks WEEK)
          AND mp.fecha_creacion IS NOT NULL
        GROUP BY YEAR(mp.fecha_creacion), WEEK(mp.fecha_creacion, 1)
        ORDER BY year ASC, week ASC
        """, nativeQuery = true)
    List<Object[]> findCatalogGrowthPerWeek(@Param("weeks") int weeks);
}
