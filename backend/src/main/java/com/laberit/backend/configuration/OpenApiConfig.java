package com.laberit.backend.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI (Swagger) para Spring Boot 3.x.
 * Define la documentación de la API y el esquema de seguridad JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Metadatos de la API
                .info(new Info()
                        .title("API de Inventario Laberit")
                        .version("1.0")
                        .description("Documentación de la API para gestión de inventario y sincronización con proveedores.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email("dev@laberit.com")))
                // Aplicar seguridad globalmente a todos los endpoints.
                // Nota: Para excluir endpoints públicos (ej. /auth/login), usar la anotación @SecurityRequirements() en el controlador o método específico.
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                // Definir los componentes de seguridad
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, createSecurityScheme()));
    }

    /**
     * Crea el esquema de seguridad HTTP Bearer con formato JWT.
     *
     * @return Objeto SecurityScheme configurado.
     */
    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
