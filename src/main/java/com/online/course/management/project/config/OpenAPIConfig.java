package com.online.course.management.project.config;

import com.online.course.management.project.constants.PathConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local server");

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        Info info = new Info()
                .title("Online Course Management System API")
                .version("1.0")
                .description("API documentation for Online Course Management System")
                .contact(new Contact()
                        .name("Development Team")
                        .email("support@example.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        Components components = new Components()
                .addSecuritySchemes("Bearer Authentication", securityScheme);

        return new OpenAPI()
                .info(info)
                .addServersItem(localServer)
                .components(components);
    }

    @Bean
    public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            boolean isPublicPath = PathConstants.PUBLIC_PATHS.stream()
                    .anyMatch(path::startsWith);

            if (!isPublicPath) {
                pathItem.readOperations().forEach(operation ->
                        operation.addSecurityItem(
                                new SecurityRequirement().addList("Bearer Authentication")
                        )
                );
            }
        });
    }
}