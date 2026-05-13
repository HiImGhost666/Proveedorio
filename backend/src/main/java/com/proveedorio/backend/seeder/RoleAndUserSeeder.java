package com.proveedorio.backend.seeder;

import com.proveedorio.backend.entity.Role;
import com.proveedorio.backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1)
public class RoleAndUserSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleAndUserSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    private void seedRoles() {
        List<String> roleNames = List.of("ROLE_ADMIN", "ROLE_CLIENTE");

        for (String roleName : roleNames) {
            if (!roleRepository.existsByName(roleName)) {
                Role newRole = new Role(roleName);
                roleRepository.save(newRole);
                System.out.println("Seeder: Rol creado -> " + roleName);
            }
        }
    }
}
