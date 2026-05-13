package com.proveedorio.backend.service;

import com.proveedorio.backend.entity.Role;
import com.proveedorio.backend.repository.RoleRepository;
import org.springframework.stereotype.Service;

/**
 * Servicio para obtener o crear roles por nombre.
 * Usado en registro de usuarios y seeders.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Obtiene un rol por nombre o lo crea si no existe.
     *
     * @param name nombre del rol (ej. ROLE_ADMIN, ROLE_CLIENTE).
     * @return la entidad {@link Role}.
     */
    public Role getOrCreateByName(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }
}

