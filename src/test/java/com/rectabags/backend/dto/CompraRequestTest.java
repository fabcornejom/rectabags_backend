package com.rectabags.backend.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CompraRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void preparar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrar() {
        factory.close();
    }

    @Test
    void aceptaDatosValidosConDominiosPermitidosYTelefonoChileno() {
        for (String correo : List.of("cliente@gmail.com", "alumno@duoc.cl", "profe@profesor.duoc.cl")) {
            assertTrue(validator.validate(compra(correo, "+56 9 1234 5678", "Región Metropolitana", 1)).isEmpty());
        }
    }

    @Test
    void rechazaCorreoDeOtroDominio() {
        assertFalse(validator.validate(compra("cliente@example.com", "912345678", "Región Metropolitana", 1)).isEmpty());
    }

    @Test
    void rechazaTelefonoInvalido() {
        assertFalse(validator.validate(compra("cliente@gmail.com", "1234", "Región Metropolitana", 1)).isEmpty());
    }

    @Test
    void rechazaRegionQueExcedeElLargoDeLaEntidad() {
        assertFalse(validator.validate(compra("cliente@gmail.com", "912345678", "Región Metropolitana de Santiago", 1)).isEmpty());
    }

    @Test
    void validaLaCantidadDeLosItemsAnidados() {
        assertFalse(validator.validate(compra("cliente@gmail.com", "912345678", "Región Metropolitana", 0)).isEmpty());
    }

    @Test
    void rechazaDatosObligatoriosVacios() {
        CompraRequest request = new CompraRequest(" ", "", "", "", "", "", "", "", List.of());
        assertFalse(validator.validate(request).isEmpty());
    }

    private CompraRequest compra(String correo, String telefono, String region, int cantidad) {
        return new CompraRequest("Marco", "Carrasco", correo, telefono, "Av. Principal 123", region,
                "Santiago", "transferencia", List.of(new ItemCompraDTO(1L, cantidad)));
    }

    @Test
    void limitesAutorizadosDeApellidoYDireccionSeValidanEnLaCompra() {
        CompraRequest limite = new CompraRequest("Marco", "a".repeat(100), "cliente@gmail.com", "912345678",
                "d".repeat(300), "Región Metropolitana", "Santiago", "transferencia", List.of(new ItemCompraDTO(1L, 1)));
        assertTrue(validator.validate(limite).isEmpty());
        assertFalse(validator.validate(new CompraRequest("Marco", "a".repeat(101), limite.correoCliente(),
                limite.telefonoCliente(), limite.direccionCliente(), limite.region(), limite.comuna(), limite.metodoPago(), limite.items())).isEmpty());
        assertFalse(validator.validate(new CompraRequest("Marco", limite.apellidoCliente(), limite.correoCliente(),
                limite.telefonoCliente(), "d".repeat(301), limite.region(), limite.comuna(), limite.metodoPago(), limite.items())).isEmpty());
    }
}
