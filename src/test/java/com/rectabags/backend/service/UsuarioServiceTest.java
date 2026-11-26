package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rectabags.backend.dto.UsuarioDTO;
import com.rectabags.backend.dto.UsuarioRequest;
import com.rectabags.backend.exception.NegocioException;
import com.rectabags.backend.model.Rol;
import com.rectabags.backend.model.Usuario;
import com.rectabags.backend.repository.UsuarioRepository;

/*
 * Pruebas unitarias de UsuarioService con Mockito.
 * El repositorio y el encriptador son "mocks": no se usa base de datos.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioRequest solicitudValida;

    @BeforeEach
    void preparar() {
        solicitudValida = new UsuarioRequest(
                "123456785", "Ana", "Pérez", "ana@gmail.com", "Clave123",
                "912345678", "Región Metropolitana", "Santiago", "Calle 1 #23",
                Rol.CLIENTE);
    }

    private Usuario usuarioExistente(Long id, Rol rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setRun("123456785");
        u.setNombre("Ana");
        u.setApellido("Pérez");
        u.setCorreo("ana@gmail.com");
        u.setContrasena("hash-viejo");
        u.setRegion("Región Metropolitana");
        u.setComuna("Santiago");
        u.setDireccion("Calle 1 #23");
        u.setRol(rol);
        return u;
    }

    // ---------------- crear ----------------

    @Test
    void crear_usuarioValido_guardaConContrasenaEncriptada() {
        when(usuarioRepository.existsByCorreo("ana@gmail.com")).thenReturn(false);
        when(usuarioRepository.existsByRun("123456785")).thenReturn(false);
        when(passwordEncoder.encode("Clave123")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.crear(solicitudValida);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        assertEquals("HASH", captor.getValue().getContrasena());          // se guardó encriptada
        assertNotEquals("Clave123", captor.getValue().getContrasena());   // nunca en texto plano
        assertEquals("ana@gmail.com", resultado.correo());
        assertEquals(Rol.CLIENTE, resultado.rol());
    }

    @Test
    void crear_correoRepetido_lanzaError() {
        when(usuarioRepository.existsByCorreo("ana@gmail.com")).thenReturn(true);

        NegocioException error = assertThrows(NegocioException.class,
                () -> usuarioService.crear(solicitudValida));

        assertEquals("Ya existe un usuario con ese correo.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_runInvalido_lanzaError() {
        UsuarioRequest conRunMalo = new UsuarioRequest(
                "123456789", "Ana", "Pérez", "ana@gmail.com", "Clave123",
                null, "Región Metropolitana", "Santiago", "Calle 1 #23", Rol.CLIENTE);

        NegocioException error = assertThrows(NegocioException.class,
                () -> usuarioService.crear(conRunMalo));

        assertEquals(true, error.getMessage().contains("RUN"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_correoConDominioNoPermitido_lanzaError() {
        UsuarioRequest conCorreoMalo = new UsuarioRequest(
                "123456785", "Ana", "Pérez", "ana@hotmail.com", "Clave123",
                null, "Región Metropolitana", "Santiago", "Calle 1 #23", Rol.CLIENTE);

        assertThrows(NegocioException.class, () -> usuarioService.crear(conCorreoMalo));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_contrasenaMuyCorta_lanzaError() {
        UsuarioRequest conClaveCorta = new UsuarioRequest(
                "123456785", "Ana", "Pérez", "ana@gmail.com", "abc",
                null, "Región Metropolitana", "Santiago", "Calle 1 #23", Rol.CLIENTE);

        assertThrows(NegocioException.class, () -> usuarioService.crear(conClaveCorta));
    }

    // ---------------- registrar ----------------

    @Test
    void registrar_siempreCreaCliente_aunqueIntentenPonerseAdmin() {
        UsuarioRequest intentoAdmin = new UsuarioRequest(
                "123456785", "Ana", "Pérez", "ana@gmail.com", "Clave123",
                null, "Región Metropolitana", "Santiago", "Calle 1 #23", Rol.ADMIN);

        when(usuarioRepository.existsByCorreo("ana@gmail.com")).thenReturn(false);
        when(usuarioRepository.existsByRun("123456785")).thenReturn(false);
        when(passwordEncoder.encode("Clave123")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.registrar(intentoAdmin);

        assertEquals(Rol.CLIENTE, resultado.rol());
    }

    // ---------------- actualizar ----------------

    @Test
    void actualizar_noPuedeCambiarSuPropioRol() {
        Usuario admin = usuarioExistente(1L, Rol.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        UsuarioRequest intentaBajarseElRol = new UsuarioRequest(
                "123456785", "Ana", "Pérez", "ana@gmail.com", null,
                null, "Región Metropolitana", "Santiago", "Calle 1 #23", Rol.CLIENTE);

        NegocioException error = assertThrows(NegocioException.class,
                () -> usuarioService.actualizar(1L, intentaBajarseElRol, 1L));

        assertEquals("No puedes cambiar tu propio rol.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_adminPuedeCambiarElRolDeOtroUsuario() {
        Usuario otro = usuarioExistente(2L, Rol.CLIENTE);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otro));
        when(usuarioRepository.existsByCorreoAndIdUsuarioNot("ana@gmail.com", 2L)).thenReturn(false);
        when(usuarioRepository.existsByRunAndIdUsuarioNot("123456785", 2L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioRequest aVendedor = new UsuarioRequest(
                "123456785", "Ana", "Pérez", "ana@gmail.com", null,
                null, "Región Metropolitana", "Santiago", "Calle 1 #23", Rol.VENDEDOR);

        UsuarioDTO resultado = usuarioService.actualizar(2L, aVendedor, 1L);

        assertEquals(Rol.VENDEDOR, resultado.rol());
        assertEquals("hash-viejo", otro.getContrasena());   // sin contraseña nueva, no cambia
    }

    // ---------------- eliminar ----------------

    @Test
    void eliminar_noPuedeEliminarSuPropiaCuenta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioExistente(1L, Rol.ADMIN)));

        NegocioException error = assertThrows(NegocioException.class,
                () -> usuarioService.eliminar(1L, 1L));

        assertEquals("No puedes eliminar tu propia cuenta.", error.getMessage());
        verify(usuarioRepository, never()).delete(any());
    }
}
