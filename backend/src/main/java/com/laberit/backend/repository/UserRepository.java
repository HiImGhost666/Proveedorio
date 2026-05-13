package com.laberit.backend.repository;

import com.laberit.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link User}.
 * Tabla en base de datos: "usuario"; columnas en español.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByActiveTrue();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
