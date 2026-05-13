package com.laberit.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro de autenticación JWT que se ejecuta una sola vez por cada solicitud HTTP.
 *
 * <p>Intercept el header {@code Authorization: Bearer <token>}, valida el JWT
 * y establece el contexto de seguridad de Spring sin consultar la base de datos.
 * Los roles se extraen directamente del claim {@code roles} del token, lo que
 * hace que este filtro sea completamente stateless y eficiente.
 *
 * <p>Se registra automáticamente en la cadena de filtros de Spring Security
 * antes del {@code UsernamePasswordAuthenticationFilter}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    /**
     * Constructor para inyección de dependencias.
     *
     * @param jwtService servicio que gestiona generación y validación de tokens JWT.
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Método principal del filtro. Procesa cada solicitud HTTP entrante para:
     * <ol>
     *   <li>Verificar la presencia del header {@code Authorization: Bearer}.</li>
     *   <li>Extraer y parsear el JWT obteniendo todos los claims en una sola operación.</li>
     *   <li>Construir el objeto {@link UserDetails} con username y roles del token.</li>
     *   <li>Validar el token y, si es válido, autenticar al usuario en el contexto de seguridad.</li>
     * </ol>
     *
     * <p>Si el token está ausente, malformado o expirado, la solicitud continúa
     * sin autenticación y Spring Security aplicará las reglas de acceso configuradas
     * en {@code SecurityConfig}.
     *
     * @param request     solicitud HTTP entrante.
     * @param response    respuesta HTTP.
     * @param filterChain cadena de filtros de la solicitud.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Si el header no existe o no tiene el esquema Bearer, continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // Extraer username; este paso también valida la firma del token
            username = jwtService.extractUsername(jwt);
        } catch (Exception ex) {
            log.warn("Token JWT inválido o malformado en request {}: {}", request.getRequestURI(), ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Solo procesar si se obtuvo un username y no hay autenticación previa en el contexto
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Obtener todos los claims del token en una sola operación de parseo.
            // Se usa extractAllClaims para no exponer la clave secreta fuera de JwtService.
            Claims claims = jwtService.extractAllClaims(jwt);

            // Extraer roles del claim personalizado y construir las authorities de Spring Security
            List<String> roles = claims.get("roles", List.class);
            UserDetails userDetails = new User(username, "", roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));

            // Validar token completo (firma + expiración + coincidencia de usuario)
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Usuario '{}' autenticado correctamente via JWT para {}", username, request.getRequestURI());
            } else {
                log.warn("Token JWT expirado o inválido para usuario '{}' en {}", username, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }
}
