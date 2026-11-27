package com.rectabags.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rectabags.backend.dto.LoginRequest;
import com.rectabags.backend.dto.LoginResponse;
import com.rectabags.backend.dto.UsuarioDTO;
import com.rectabags.backend.dto.UsuarioRequest;
import com.rectabags.backend.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Devuelve { token, usuario }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    // Crea un CLIENTE. No inicia sesión: el usuario debe hacer login después.
    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registro(@RequestBody UsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registro(req));
    }
}
