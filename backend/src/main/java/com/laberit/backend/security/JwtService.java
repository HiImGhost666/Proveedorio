package com.laberit.backend.security;

import com.laberit.backend.entity.User;
import com.laberit.backend.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio responsable de la generación y validación de tokens JWT.
 *
 * <p>Usa el algoritmo HMAC-SHA256 (HS256). La clave secreta se inyecta
 * en tiempo de arranque desde HashiCorp Vault a través de Spring Cloud Vault,
 * usando la propiedad {@code jwt.secret-key} almacenada en {@code secret/inventario}.
 *
 * <p>Al ser un componente Spring ({@code @Component}), está disponible
 * para inyección de dependencias en el filtro {@code JwtAuthenticationFilter}
 * y en los controladores de autenticación.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /**
     * Clave secreta para HMAC-SHA256, inyectada desde Vault via Spring Cloud Vault.
     * La propiedad {@code jwt.secret-key} es sembrada en Vault por el servicio
     * vault-init al arrancar el stack y debe tener al menos 32 caracteres para HS256.
     *
     * <p>IMPORTANTE: este campo reemplaza la clave hardcodeada anterior (Bug C1).
     * Nunca incluir el valor real de esta clave en el código fuente ni en commits.
     */
    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    private final UserRepository userRepository;

    /**
     * Tiempo de expiración de los tokens JWT: 150 minutos.
     * Modificar este valor implica reconstruir el backend.
     * En el futuro puede externalizar esta configuración a Vault o application.properties.
     */
    private final long EXPIRATION_TIME_MS = 1000L * 60 * 150;

    /**
     * Constructor para inyección de dependencias.
     *
     * @param userRepository repositorio de usuarios necesario para incluir
     *                       el nombre completo como claim del token.
     */
    public JwtService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Construye y devuelve el {@link SecretKey} a partir de la clave inyectada por Vault.
     *
     * <p>Se construye en cada llamada en lugar de cachearse como campo final porque
     * {@code @Value} no está disponible en el momento de inicialización del campo.
     * El coste de construir el SecretKey es mínimo (operación en memoria).
     *
     * @return la clave secreta lista para firmar o verificar tokens JWT.
     * @throws IllegalStateException si {@code jwt.secret-key} no fue inyectada correctamente.
     */
    private SecretKey getSecretKey() {
        if (jwtSecretKey == null || jwtSecretKey.isBlank()) {
            log.error("jwt.secret-key no está configurada. Verifica que Vault esté activo y que vault-init haya completado correctamente.");
            throw new IllegalStateException("La clave JWT no está disponible. El servicio Vault puede no estar accesible.");
        }
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Devuelve el tiempo de expiración configurado en milisegundos.
     * Útil para construir el campo {@code expiresIn} del {@code AuthResponse}.
     *
     * @return tiempo de expiración en milisegundos.
     */
    public Long getExpirationTime() {
        return EXPIRATION_TIME_MS;
    }

    /**
     * Genera un nuevo token JWT firmado para el usuario dado.
     *
     * <p>El token incluye los siguientes claims:
     * <ul>
     *   <li>{@code jti} — ID único (UUID) para prevenir la reutilización del token.</li>
     *   <li>{@code sub} — nombre de usuario (subject).</li>
     *   <li>{@code iat} — fecha de emisión.</li>
     *   <li>{@code exp} — fecha de expiración (150 minutos desde la emisión).</li>
     *   <li>{@code iss} — emisor: "Laberit".</li>
     *   <li>{@code aud} — audiencia: "WebApiInventario".</li>
     *   <li>{@code roles} — lista de roles del usuario extraídos de {@link UserDetails}.</li>
     *   <li>{@code fullName} — nombre completo del usuario.</li>
     * </ul>
     *
     * @param userDetails información del usuario autenticado.
     * @return token JWT compacto firmado con HMAC-SHA256.
     * @throws RuntimeException si el usuario no existe en la base de datos.
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME_MS);

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado con username: " + userDetails.getUsername()));

        log.debug("Generando token JWT para usuario: {}", userDetails.getUsername());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiration)
                .issuer("Laberit")
                .audience().add("WebApiInventario").and()
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("fullName", user.getFullName())
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parsea y devuelve todos los claims del payload de un token JWT.
     *
     * <p>Este método valida la firma del token como efecto secundario del parseo.
     * Es utilizado por {@code JwtAuthenticationFilter} para extraer roles y otros
     * claims personalizados sin necesidad de acceder directamente a la clave secreta.
     *
     * @param token token JWT compacto.
     * @return objeto {@link Claims} con todos los claims del payload.
     * @throws io.jsonwebtoken.JwtException si el token es inválido o la firma no coincide.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el nombre de usuario (claim {@code sub}) de un token JWT.
     *
     * <p>Este método también valida la firma del token. Si la firma no es válida
     * o el token está malformado, JJWT lanzará una excepción.
     *
     * @param token token JWT compacto.
     * @return nombre de usuario contenido en el claim {@code subject}.
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Verifica si un token JWT es válido para el usuario dado.
     *
     * <p>Un token es válido si:
     * <ul>
     *   <li>El claim {@code subject} coincide con el username de {@code userDetails}.</li>
     *   <li>La fecha de expiración es posterior al momento actual.</li>
     *   <li>La firma HMAC-SHA256 es correcta (verificada implícitamente al parsear).</li>
     * </ul>
     *
     * @param token       token JWT a verificar.
     * @param userDetails información del usuario contra el que se valida.
     * @return {@code true} si el token es válido; {@code false} en caso contrario.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        Date expiration = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        boolean valid = username.equals(userDetails.getUsername()) && expiration.after(new Date());
        if (!valid) {
            log.debug("Token inválido para usuario '{}': expirado={}", username, !expiration.after(new Date()));
        }
        return valid;
    }
}
