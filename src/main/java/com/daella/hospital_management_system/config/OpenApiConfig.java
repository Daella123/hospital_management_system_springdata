package com.daella.hospital_management_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc OpenAPI configuration.
 * Swagger UI accessible at: http://localhost:8080/swagger-ui.html
 * API docs at:              http://localhost:8080/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hospitalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Management System API")
                        .version("1.0.0")
                        .description("""
                                REST API for the Hospital Management System.
                                
                                Supports full management of:
                                - Patients, Doctors, Departments
                                - Appointments (scheduling, filtering, status updates)
                                - Prescriptions and items
                                - Patient feedback and ratings
                                - Medical inventory with low-stock alerts
                                
                                All endpoints return a consistent ApiResponse wrapper:
                                { status, message, data, timestamp }
                                """)
                        .contact(new Contact()
                                .name("Daella Dev")
                                .email("daella@hospital.dev"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development Server"),
                        new Server().url("https://hospital.daella.dev").description("Production Server")));
    }
}
