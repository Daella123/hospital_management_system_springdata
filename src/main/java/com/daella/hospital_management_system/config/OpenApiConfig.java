package com.daella.hospital_management_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc OpenAPI configuration.
 *
 * <p>Swagger UI accessible at: <a href="http://localhost:8080/swagger-ui.html">swagger-ui.html</a> (no auth required)<br>
 * API docs at: <a href="http://localhost:8080/api-docs">api-docs</a></p>
 *
 * <p>JWT bearer token support is configured via the "bearerAuth" security scheme.
 * Click the <b>Authorize</b> button in Swagger UI, paste your token, and all
 * secured endpoints will include the {@code Authorization: Bearer} header automatically.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI hospitalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Management System API")
                        .version("2.0.0")
                        .description("""
                                REST API for the Hospital Management System — secured with Spring Security & JWT.
                                
                                **Authentication:**
                                1. Register via `POST /auth/register`
                                2. Login via `POST /auth/login` to receive a JWT token
                                3. Click **Authorize** above, enter: `<your-token>` (without "Bearer ")
                                4. All secured endpoints will now include the token automatically
                                
                                **Roles:** ADMIN · DOCTOR · NURSE · RECEPTIONIST
                                
                                All endpoints return a consistent ApiResponse wrapper:
                                `{ status, message, data, timestamp }`
                                """)
                        .contact(new Contact()
                                .name("Daella Dev")
                                .email("daella@hospital.dev"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development Server"),
                        new Server().url("https://hospital.daella.dev").description("Production Server")))
                // JWT Bearer security scheme — enables the Authorize button in Swagger UI
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here (without the 'Bearer ' prefix)")))
                // Apply bearer auth globally
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
