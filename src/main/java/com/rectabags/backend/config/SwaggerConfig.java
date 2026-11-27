package com.rectabags.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI rectabagsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RectaBags API")
                        .version("1.0")
                        .description("API REST de la tienda RectaBags: catálogo, usuarios, "
                                + "autenticación con JWT, compras y reportes. "
                                + "Proyecto DSY1104 – Desarrollo Fullstack II, Duoc UC.")
                        .contact(new Contact()
                                .name("Equipo RectaBags")));
    }
}