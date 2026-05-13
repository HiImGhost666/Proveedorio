package com.proveedorio.backend.service;

import com.proveedorio.backend.dtos.auth.UserDto;
import com.proveedorio.backend.entity.Role;
import com.proveedorio.backend.entity.User;
import com.proveedorio.backend.repository.RoleRepository;
import com.proveedorio.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para usuarios del sistema.
 * Gestiona CRUD de usuarios, carga por username para Spring Security y conversión a DTOs.
 * Usado por {@link com.proveedorio.backend.controller.UserController} y {@link com.proveedorio.backend.controller.AuthController}.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + username));
    }

    /**
     * Obtiene todos los usuarios mapeados a DTO.
     */
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Busca un usuario por ID y lo devuelve como DTO.
     */
    public Optional<UserDto> findById(Integer id) {
        return userRepository.findById(id).map(this::convertToDto);
    }

    /**
     * Crea un nuevo usuario a partir del DTO. Codifica la contraseña si se proporciona.
     */
    public UserDto save(UserDto userDto) {
        validateRoles(userDto.roles(), true);
        User user = convertToEntity(userDto);
        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }
        User saved = userRepository.save(user);
        return convertToDto(saved);
    }

    /**
     * Actualiza un usuario existente. Solo modifica los campos no nulos del DTO.
     */
    public UserDto update(Integer id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        if (userDto.username() != null) user.setUsername(userDto.username());
        if (userDto.email() != null) user.setEmail(userDto.email());
        if (userDto.fullName() != null) user.setFullName(userDto.fullName());

        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }

        if (userDto.roles() != null) {
            validateRoles(userDto.roles(), false);
            user.setRoles(userDto.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + roleName)))
                    .collect(Collectors.toSet()));
        }

        User updated = userRepository.save(user);
        return convertToDto(updated);
    }

    /**
     * Elimina un usuario por ID.
     */
    public void deleteById(Integer id) {
        userRepository.deleteById(id);
    }

    /**
     * Actualiza el perfil del usuario autenticado (email, nombre completo, contraseña).
     */
    public UserDto updateProfile(String username, UserDto userDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + username));

        if (userDto.email() != null) user.setEmail(userDto.email());
        if (userDto.fullName() != null) user.setFullName(userDto.fullName());
        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }

        User updated = userRepository.save(user);
        return convertToDto(updated);
    }

    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                null,
                user.getRoles() != null
                        ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                        : new HashSet<>()
        );
    }

    private User convertToEntity(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setFullName(userDto.fullName());

        if (userDto.roles() != null) {
            user.setRoles(userDto.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + roleName)))
                    .collect(Collectors.toSet()));
        } else {
            user.setRoles(new HashSet<>());
        }
        return user;
    }

    private void validateRoles(java.util.Set<String> roles, boolean required) {
        if (required && (roles == null || roles.isEmpty())) {
            throw new IllegalArgumentException("El usuario debe tener exactamente un rol.");
        }
        if (roles != null && roles.size() > 1) {
            throw new IllegalArgumentException("El usuario solo puede tener un rol.");
        }
    }
}

