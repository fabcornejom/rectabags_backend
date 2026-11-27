package com.rectabags.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rectabags.backend.dto.LoginRequest;
import com.rectabags.backend.dto.LoginResponse;
import com.rectabags.backend.dto.UsuarioDTO;
import com.rectabags.backend.dto.UsuarioRequest;
import com.rectabags.backend.exception.CredencialesInvalidasException;
import com.rectabags.backend.model.Usuario;
import com.rectabags.backend.repository.UsuarioRepository;
import com.rectabags.backend.security.JwtUtil;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            UsuarioService usuarioService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
    }

    public LoginResponse login(LoginRequest req) {

        if (req == null || req.correo() == null || req.contrasena() == null) {
            throw new CredencialesInvalidasException();
        }

        String correo = req.correo().trim().toLowerCase();

        // Mismo error para "correo no existe" y "contraseña mala":
        // así no se le dice a un atacante qué correos están registrados.
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(req.contrasena(), usuario.getContrasena())) {
            throw new CredencialesInvalidasException();
        }

        return new LoginResponse(jwtUtil.generarToken(usuario), UsuarioDTO.de(usuario));
    }

    public UsuarioDTO registro(UsuarioRequest req) {
        return usuarioService.registrar(req);
    }
}
