package com.laberit.backend.repository;

import com.laberit.backend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Supplier}.
 * La tabla en base de datos es "proveedores"; las columnas se mantienen en español.
 */
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    Optional<Supplier> findByName(String name);

    Optional<Supplier> findByNameIgnoreCase(String name);

    List<Supplier> findByActiveTrue();

    @Query("SELECT DISTINCT s FROM Supplier s LEFT JOIN FETCH s.mappingConfigurations WHERE s.active = true")
    List<Supplier> findAllByActiveTrueWithMappings();

    boolean existsByName(String name);

    @Query("SELECT s FROM Supplier s JOIN FETCH s.mappingConfigurations WHERE s.name = :name")
    Optional<Supplier> findByNameWithMappings(@Param("name") String name);

    @Query("SELECT s FROM Supplier s LEFT JOIN FETCH s.mappingConfigurations WHERE s.id = :id")
    Optional<Supplier> findByIdWithMappings(@Param("id") Integer id);
}
