package com.rectabags.backend.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CorsConfiguracionTest {
    @Test
    void laListaConfiguradaSustituyeLosOrigenesLocalesYEliminaEspacios() {
        var source = new SecurityConfig().corsConfigurationSource(" https://tienda.example , , https://otra.example ");
        var config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/productos"));
        assertNotNull(config);
        assertEquals("https://tienda.example", config.checkOrigin("https://tienda.example"));
        assertEquals("https://otra.example", config.checkOrigin("https://otra.example"));
        assertNull(config.checkOrigin("http://localhost:5173"));
        assertNull(config.checkOrigin("http://localhost:4173"));
        assertNull(config.checkOrigin("https://ajena.example"));
    }

    @Test
    void unaListaVaciaNoAutorizaOrigenes() {
        var source = new SecurityConfig().corsConfigurationSource(" , ");
        var config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/productos"));
        assertNotNull(config);
        assertNull(config.checkOrigin("http://localhost:5173"));
    }
}
