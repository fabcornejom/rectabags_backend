package com.rectabags.backend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class CorsHttpTest {
    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:4173"})
    void catalogoPublicoPermiteDesarrolloYVistaPrevia(String origen) throws Exception {
        mvc.perform(get("/api/v1/productos").header("Origin", origen))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origen));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:4173"})
    void preflightPermiteAuthorizationSinEjecutarLaOperacionProtegida(String origen) throws Exception {
        mvc.perform(options("/api/v1/usuarios").header("Origin", origen)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origen))
                .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:4173"})
    void origenPermitidoNoReemplazaLaAutenticacion(String origen) throws Exception {
        mvc.perform(get("/api/v1/usuarios").header("Origin", origen))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Access-Control-Allow-Origin", origen));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:4173"})
    void origenPermitidoNoOtorgaPermisosAdministrativosAUnCliente(String origen) throws Exception {
        mvc.perform(get("/api/v1/usuarios").header("Origin", origen).with(user("999998").roles("CLIENTE")))
                .andExpect(status().isForbidden()).andExpect(header().string("Access-Control-Allow-Origin", origen));
    }

    @Test
    void rechazaUnOrigenAjenoEnConsultas() throws Exception {
        mvc.perform(get("/api/v1/productos").header("Origin", "http://localhost:9999"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void rechazaUnOrigenAjenoEnPreflight() throws Exception {
        mvc.perform(options("/api/v1/usuarios").header("Origin", "http://localhost:9999")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
