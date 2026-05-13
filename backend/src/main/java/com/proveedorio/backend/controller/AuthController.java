package com.proveedorio.backend.controller;


import com.proveedorio.backend.dtos.auth.*;
import com.proveedorio.backend.entity.Role;
import com.proveedorio.backend.entity.User;
import com.proveedorio.backend.repository.UserRepository;
import com.proveedorio.backend.security.JwtService;
import com.proveedorio.backend.service.RoleService;
import com.proveedorio.backend.service.UserService;
import com.proveedorio.backend.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador REST de autenticación: login, registro, validación y renovación de token JWT.
 * Rutas bajo /api/v1/auth. Mensajes de error en español.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserService userService, UserRepository userRepository,
                          PasswordEncoder passwordEncoder, RoleService roleService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
    }

    // Endpoint para el login de usuarios.
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequestDto loginRequestDTO) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.username(), loginRequestDTO.password())
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            AuthResponse authResponse = new AuthResponse(
                    token,
                    "Bearer",
                    jwtService.getExpirationTime(),
                    userDetails.getUsername(),
                    userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
            );

            return ResponseEntity.ok(ApiResponse.success(authResponse, "Sesión iniciada correctamente"));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Credenciales inválidas"));
        }
    }

    // Endpoint para registrar un nuevo usuario.
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody RegisterRequestDto registerRequestDTO) {
        if (userRepository.existsByUsername(registerRequestDTO.username())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("El username ya existe"));
        }

        String passwordEnc = passwordEncoder.encode(registerRequestDTO.password());
        Role role = roleService.getOrCreateByName("ROLE_BASIC");
        Set<Role> roles = new HashSet<>(Collections.singletonList(role));

        User newUser = new User();
        newUser.setUsername(registerRequestDTO.username());
        newUser.setEmail(registerRequestDTO.email());
        newUser.setPassword(passwordEnc);
        newUser.setFullName(registerRequestDTO.fullName());
        newUser.setRoles(roles);
        newUser.setActive(true);

        userRepository.save(newUser);

        UserResponseDto response = new UserResponseDto(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getEmail(),
                newUser.getFullName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Usuario registrado con éxito"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestParam("token") String token) {
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userService.loadUserByUsername(username);
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            return ResponseEntity.ok(ApiResponse.success(isValid, "Verificación completada"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(false, "Token no válido o expirado"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            // Extraemos usuario del token actual
            String username = jwtService.extractUsername(request.refreshToken());
            UserDetails userDetails = userService.loadUserByUsername(username);

            // Generamos un nuevo token
            String newToken = jwtService.generateToken(userDetails);

            AuthResponse authResponse = new AuthResponse(
                    newToken,
                    "Bearer",
                    jwtService.getExpirationTime(),
                    userDetails.getUsername(),
                    userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
            );

            return ResponseEntity.ok(ApiResponse.success(authResponse, "Token renovado con éxito"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No se pudo renovar el token"));
        }
    }

}
