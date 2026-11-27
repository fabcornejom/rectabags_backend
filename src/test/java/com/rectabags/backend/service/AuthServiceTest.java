package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rectabags.backend.dto.LoginRequest;
import com.rectabags.backend.dto.LoginResponse;
import com.rectabags.backend.exception.CredencialesInvalidasException;
import com.rectabags.backend.model.Rol;
import com.rectabags.backend.model.Usuario;
import com.rectabags.backend.repository.UsuarioRepository;
import com.rectabags.backend.security.JwtUtil;

/*
 * Pruebas del login. El login incorrecto lanza CredencialesInvalidasException,
 * que GlobalExceptionHandler convierte en una respuesta 401.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private AuthService authService;

    private Usuario admin() {
        Usuario u = new Usuario();
        u.setIdUsuario(1L);
        u.setNombre("Admin");
        u.setApellido("RectaBags");
        u.setCorreo("admin@duoc.cl");
        u.setContrasena("HASH");
        u.setRol(Rol.ADMIN);
        return u;
    }

    @Test
    void login_correcto_devuelveTokenYUsuario() {
        Usuario admin = admin();
        when(usuarioRepository.findByCorreo("admin@duoc.cl")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Admin123!", "HASH")).thenReturn(true);
        when(jwtUtil.generarToken(admin)).thenReturn("token-de-prueba");

        LoginResponse respuesta = authService.login(new LoginRequest("admin@duoc.cl", "Admin123!"));

        assertEquals("token-de-prueba", respuesta.token());
        assertEquals("admin@duoc.cl", respuesta.usuario().correo());
        assertEquals(Rol.ADMIN, respuesta.usuario().rol());
    }

    @Test
    void login_correoConMayusculasYEspacios_seNormaliza() {
        Usuario admin = admin();
        when(usuarioRepository.findByCorreo("admin@duoc.cl")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Admin123!", "HASH")).thenReturn(true);
        when(jwtUtil.generarToken(admin)).thenReturn("token");

        LoginResponse respuesta = authService.login(new LoginRequest("  ADMIN@duoc.CL ", "Admin123!"));

        assertEquals("token", respuesta.token());
    }

    @Test
    void login_contrasenaIncorrecta_lanzaCredencialesInvalidas() {
        when(usuarioRepository.findByCorreo("admin@duoc.cl")).thenReturn(Optional.of(admin()));
        when(passwordEncoder.matches("mala", "HASH")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequest("admin@duoc.cl", "mala")));
    }

    @Test
    void login_correoInexistente_lanzaCredencialesInvalidas() {
        when(usuarioRepository.findByCorreo("nadie@gmail.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequest("nadie@gmail.com", "Clave123")));
    }
}
