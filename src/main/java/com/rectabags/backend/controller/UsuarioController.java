package com.rectabags.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rectabags.backend.dto.UsuarioDTO;
import com.rectabags.backend.dto.UsuarioRequest;
import com.rectabags.backend.service.UsuarioService;

/*
 * Quién puede entrar a cada ruta lo decide SecurityConfig:
 *  - /usuarios/me  -> cualquier usuario logueado
 *  - el resto      -> solo ADMIN
 * authentication.getName() es el id del usuario (lo pone JwtFilter).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ----- Mi perfil -----

    @GetMapping("/me")
    public UsuarioDTO miPerfil(Authentication authentication) {
        return usuarioService.perfil(idActual(authentication));
    }

    @PutMapping("/me")
    public UsuarioDTO editarMiPerfil(
            Authentication authentication,
            @RequestBody UsuarioRequest req) {

        return usuarioService.actualizarPerfil(idActual(authentication), req);
    }

    // ----- Administración (ADMIN) -----

    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioDTO obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(@RequestBody UsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(req));
    }

    @PutMapping("/{id}")
    public UsuarioDTO actualizar(
            @PathVariable Long id,
            @RequestBody UsuarioRequest req,
            Authentication authentication) {

        return usuarioService.actualizar(id, req, idActual(authentication));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> eliminar(
            @PathVariable Long id,
            Authentication authentication) {

        usuarioService.eliminar(id, idActual(authentication));
        return Map.of("mensaje", "Usuario eliminado.");
    }

    private Long idActual(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
