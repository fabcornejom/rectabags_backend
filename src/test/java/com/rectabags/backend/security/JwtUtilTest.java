package com.rectabags.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.rectabags.backend.model.Rol;
import com.rectabags.backend.model.Usuario;

class JwtUtilTest {

    private static final String SECRETO = "secreto-de-prueba-que-tiene-mas-de-32-caracteres";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setIdUsuario(7L);
        u.setCorreo("ana@gmail.com");
        u.setRol(Rol.CLIENTE);
        return u;
    }

    @Test
    void tokenValido_devuelveElIdDelUsuario() {
        JwtUtil jwtUtil = new JwtUtil(SECRETO, 60_000);

        String token = jwtUtil.generarToken(usuario());

        assertEquals(7L, jwtUtil.idDesdeToken(token).orElseThrow());
    }

    @Test
    void tokenAlterado_sinFirmaValida_esRechazado() {
        JwtUtil jwtUtil = new JwtUtil(SECRETO, 60_000);

        String token = jwtUtil.generarToken(usuario());
        String alterado = token.substring(0, token.length() - 3) + "abc";

        assertTrue(jwtUtil.idDesdeToken(alterado).isEmpty());
    }

    @Test
    void tokenVencido_esRechazado() {
        JwtUtil jwtUtil = new JwtUtil(SECRETO, -1_000);   // ya nace vencido

        String token = jwtUtil.generarToken(usuario());

        assertTrue(jwtUtil.idDesdeToken(token).isEmpty());
    }

    @Test
    void tokenFirmadoConOtraClave_esRechazado() {
        String token = new JwtUtil("otra-clave-distinta-con-mas-de-32-caracteres!!", 60_000)
                .generarToken(usuario());

        assertTrue(new JwtUtil(SECRETO, 60_000).idDesdeToken(token).isEmpty());
    }

    @Test
    void textoQueNoEsToken_esRechazado() {
        JwtUtil jwtUtil = new JwtUtil(SECRETO, 60_000);

        assertTrue(jwtUtil.idDesdeToken("esto-no-es-un-token").isEmpty());
    }
}
