package com.proveedorio.backend.repository;

import com.proveedorio.backend.entity.GlobalSyncState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GlobalSyncStateRepository extends JpaRepository<GlobalSyncState, Long> {

    /**
     * Inicializa de forma idempotente la fila de estado global (id=1).
     * <p>
     * Usa <code>INSERT IGNORE</code> para que múltiples réplicas puedan llamar
     * al método simultáneamente sin lanzar excepciones de clave duplicada ni
     * corromper la sesión de Hibernate.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO global_sync_state (id, status, updated_at) VALUES (1, 'IDLE', NOW())", nativeQuery = true)
    void ensureRowExists();

    /**
     * Recupera la fila de estado aplicando bloqueo pesimista (SELECT FOR UPDATE).
     * <p>
     * Esto garantiza que solo una transacción pueda modificar el estado a la vez,
     * implementando un patrón de "candado distribuido" a nivel de base de datos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GlobalSyncState g WHERE g.id = :id")
    Optional<GlobalSyncState> findByIdForUpdate(@Param("id") Long id);

}
