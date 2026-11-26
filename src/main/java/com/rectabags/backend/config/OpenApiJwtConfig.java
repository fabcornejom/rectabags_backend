package com.rectabags.backend.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/*
 * OPCIONAL. Agrega el botón "Authorize" a Swagger para pegar el token
 * y probar las rutas protegidas. Si da error al compilar, borra este archivo:
 * el resto del proyecto funciona igual (puedes probar con Postman).
 */
@Configuration
public class OpenApiJwtConfig {

    @Bean
    public OpenApiCustomizer jwtCustomizer() {
        return openApi -> {

            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }

            openApi.getComponents().addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"));

            openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        };
    }
}
