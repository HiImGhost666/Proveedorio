package com.laberit.backend.repository;

import com.laberit.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio de acceso a datos para la entidad {@link Role}.
 * Tabla en base de datos: "roles"; columnas en español.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);

    Optional<Role> findByNameIgnoreCase(String name);

    Set<Role> findByCreatedAtAfter(LocalDateTime date);

    Set<Role> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByName(String name);
}
