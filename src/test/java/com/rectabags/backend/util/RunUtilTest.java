package com.rectabags.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RunUtilTest {

    @Test
    void normalizar_quitaPuntosGuionYEspacios() {
        assertEquals("123456785", RunUtil.normalizar("12.345.678-5"));
        assertEquals("10000013K", RunUtil.normalizar(" 10.000.013-k "));
    }

    @Test
    void esValido_aceptaRunCorrecto() {
        assertTrue(RunUtil.esValido("111111111"));
        assertTrue(RunUtil.esValido("123456785"));
        assertTrue(RunUtil.esValido("201234565"));
    }

    @Test
    void esValido_aceptaDigitoKYCero() {
        assertTrue(RunUtil.esValido("10000013K"));
        assertTrue(RunUtil.esValido("100000040"));
    }

    @Test
    void esValido_aceptaRunDe7Caracteres() {
        assertTrue(RunUtil.esValido("1234560"));
    }

    @Test
    void esValido_rechazaDigitoVerificadorIncorrecto() {
        assertFalse(RunUtil.esValido("123456789"));
        assertFalse(RunUtil.esValido("19011022K"));
    }

    @Test
    void esValido_rechazaLargoIncorrectoOLetras() {
        assertFalse(RunUtil.esValido("12345"));          // muy corto
        assertFalse(RunUtil.esValido("1234567890"));     // muy largo
        assertFalse(RunUtil.esValido("12A45678K"));      // letras en el cuerpo
        assertFalse(RunUtil.esValido(""));
        assertFalse(RunUtil.esValido(null));
    }
}
