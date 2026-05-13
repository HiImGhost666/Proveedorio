package com.laberit.backend.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.laberit.backend.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthFilter,
                                           AuthenticationProvider authenticationProvider) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Habilitar CORS
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Actuator health endpoint (used by Docker healthcheck)
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()

                        // Rutas públicas de autenticación
                        .requestMatchers("/auth/**", "/api/v1/auth/**").permitAll()
                        
                        // Rutas públicas de Swagger / OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Rutas de productos (API v1)
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**").hasAnyRole("ADMIN", "CLIENTE")
                        .requestMatchers("/api/v1/productos/**").hasRole("ADMIN")

                        // Rutas de proveedores (API v1)
                        // Las reglas específicas deben ir antes de las generales
                        .requestMatchers(HttpMethod.GET, "/api/v1/proveedores/list").hasAnyRole("ADMIN", "CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/proveedores/*/dashboard").hasAnyRole("ADMIN", "CLIENTE")
                        .requestMatchers("/api/v1/proveedores/**").hasRole("ADMIN")

                        // Rutas de administración de usuarios
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Ruta para que el usuario actualice su perfil
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/perfil").authenticated()

                        // Notificaciones globales
                        .requestMatchers("/api/v1/notificaciones/**").authenticated()
                        
                        // Resto requiere autenticación
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // allowedOriginPatterns is required when allowCredentials is true (allowedOrigins("*") is incompatible)
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
