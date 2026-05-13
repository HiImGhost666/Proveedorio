package com.laberit.backend.controller;

import com.laberit.backend.dtos.auth.UserDto;
import com.laberit.backend.service.UserService;
import com.laberit.backend.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de usuarios (solo administradores) y actualización de perfil.
 * Las rutas se mantienen bajo /api/v1/admin/usuarios y /api/v1/usuarios/perfil por compatibilidad con el frontend.
 */
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lista todos los usuarios. Solo ADMIN.
     */
    @GetMapping("/admin/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> list = userService.findAll();
        return ResponseEntity.ok(ApiResponse.success(list, "Lista de usuarios recuperada con éxito"));
    }

    /**
     * Crea un nuevo usuario. Solo ADMIN.
     */
    @PostMapping("/admin/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody UserDto userDto) {
        UserDto created = userService.save(userDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Usuario creado con éxito"));
    }

    /**
     * Obtiene un usuario por ID. Solo ADMIN.
     */
    @GetMapping("/admin/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Integer id) {
        return userService.findById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto, "Usuario encontrado")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Usuario no encontrado")));
    }

    /**
     * Actualiza un usuario existente. Solo ADMIN.
     */
    @PutMapping("/admin/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Integer id, @RequestBody UserDto userDto) {
        UserDto updated = userService.update(id, userDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Usuario actualizado con éxito"));
    }

    /**
     * Elimina un usuario por ID. Solo ADMIN.
     */
    @DeleteMapping("/admin/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Actualiza el perfil del usuario autenticado (email, nombre completo, contraseña).
     */
    @PutMapping("/usuarios/perfil")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(Authentication authentication, @RequestBody UserDto userDto) {
        String username = authentication.getName();
        UserDto updated = userService.updateProfile(username, userDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Perfil actualizado con éxito"));
    }
}
