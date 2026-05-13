package com.laberit.backend.service;

import com.laberit.backend.dtos.SyncStatus;
import com.laberit.backend.entity.GlobalSyncState;
import com.laberit.backend.repository.GlobalSyncStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio que gestiona el estado global de sincronización en base de datos.
 * Todas las réplicas del backend consultan y actualizan la misma fila de {@link GlobalSyncState},
 * de forma que solo una pueda ejecutar la sincronización masiva a la vez.
 *
 * Utiliza un bloqueo pesimista (SELECT FOR UPDATE) para implementar un patrón
 * de "candado distribuido" sin depender de memoria local ni afinidad de sesión.
 */
@Service
public class SyncStateService {

    private final GlobalSyncStateRepository repository;

    public SyncStateService(GlobalSyncStateRepository repository) {
        this.repository = repository;
    }

    /**
     * Obtiene el estado actual de la sincronización global.
     * Si aún no existe la fila en base de datos, se asume estado IDLE por defecto.
     */
    @Transactional(readOnly = true)
    public SyncStatus getStatus() {
        return repository.findById(1L)
                .map(GlobalSyncState::getStatus)
                .orElse(SyncStatus.IDLE);
    }

    /**
     * Intenta adquirir el "candado" de sincronización (pasar a estado IN_PROGRESS).
     * <p>
     * - Si ninguna otra réplica está sincronizando, marca el estado como IN_PROGRESS y devuelve true.<br>
     * - Si ya hay una sincronización en curso, devuelve false y la llamada debe abortar.
     */
    /**
     * Actualiza el estado global de sincronización (por ejemplo a COMPLETED o ERROR).
     * Se ejecuta en una nueva transacción para garantizar que se persista incluso
     * si la llamada original falla.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquireLock() {
        repository.ensureRowExists();
        GlobalSyncState state = repository.findByIdForUpdate(1L).orElseThrow();
        if (state.getStatus() != SyncStatus.IN_PROGRESS) {
            state.setStatus(SyncStatus.IN_PROGRESS);
            
            repository.save(state);
            return true;
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setStatus(SyncStatus status) {
        GlobalSyncState state = repository.findById(1L).orElseThrow();
        state.setStatus(status);
        
        repository.save(state);
    }
}

