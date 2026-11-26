package com.rectabags.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.rectabags.backend.dto.CompraRequest;
import com.rectabags.backend.service.BoletaService;

class BoletaControllerTest {

    private BoletaService service;
    private MockMvc mvc;

    private static final String COMPRA_VALIDA = """
            {
              "nombreCliente": "Marco",
              "apellidoCliente": "Carrasco",
              "correoCliente": "cliente@gmail.com",
              "telefonoCliente": "912345678",
              "direccionCliente": "Av. Principal 123",
              "region": "Región Metropolitana",
              "comuna": "Santiago",
              "metodoPago": "transferencia",
              "items": [{"idProducto": 1, "cantidad": 2}]
            }
            """;

    @BeforeEach
    void preparar() {
        service = mock(BoletaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new BoletaController(service)).build();
    }

    @Test
    void compraValidaRespondeCreadoEnLaRutaVersionada() throws Exception {
        mvc.perform(post("/api/v1/boletas").contentType(MediaType.APPLICATION_JSON).content(COMPRA_VALIDA))
                .andExpect(status().isCreated());
        verify(service).crearCompra(any(CompraRequest.class));
    }

    @Test
    void faltaStockResponde400ConMensajeDelProducto() throws Exception {
        when(service.crearCompra(any())).thenThrow(new IllegalArgumentException("Stock insuficiente para Mochila."));

        mvc.perform(post("/api/v1/boletas").contentType(MediaType.APPLICATION_JSON).content(COMPRA_VALIDA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente para Mochila."));
    }

    @Test
    void correoNoPermitidoResponde400SinRegistrarCompra() throws Exception {
        mvc.perform(post("/api/v1/boletas").contentType(MediaType.APPLICATION_JSON)
                        .content(COMPRA_VALIDA.replace("cliente@gmail.com", "cliente@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void cantidadCeroResponde400SinRegistrarCompra() throws Exception {
        mvc.perform(post("/api/v1/boletas").contentType(MediaType.APPLICATION_JSON)
                        .content(COMPRA_VALIDA.replace("\"cantidad\": 2", "\"cantidad\": 0")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
