package com.rectabags.backend.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rectabags.backend.dto.UsuarioDTO;
import com.rectabags.backend.dto.UsuarioRequest;
import com.rectabags.backend.exception.NegocioException;
import com.rectabags.backend.exception.RecursoNoEncontradoException;
import com.rectabags.backend.model.Rol;
import com.rectabags.backend.model.Usuario;
import com.rectabags.backend.repository.UsuarioRepository;
import com.rectabags.backend.util.RunUtil;

/*
 * Toda la lógica de usuarios y TODAS sus validaciones están aquí,
 * en un solo lugar, para poder probarlas con Mockito.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------
    // Registro público y creación por el ADMIN
    // ------------------------------------------------------------------

    /** Registro público: el rol SIEMPRE es CLIENTE, aunque el JSON diga otra cosa. */
    @Transactional
    public UsuarioDTO registrar(UsuarioRequest req) {
        return crearUsuario(req, Rol.CLIENTE);
    }

    /** Creación por el ADMIN: puede elegir cualquier rol. */
    @Transactional
    public UsuarioDTO crear(UsuarioRequest req) {

        if (req.rol() == null) {
            throw new NegocioException("El tipo de usuario es obligatorio.");
        }

        return crearUsuario(req, req.rol());
    }

    private UsuarioDTO crearUsuario(UsuarioRequest req, Rol rol) {

        String run = RunUtil.normalizar(req.run());
        String correo = normalizarCorreo(req.correo());

        validarRun(run);
        validarCorreo(correo);
        validarNombreYApellido(req);
        validarContrasena(req.contrasena(), true);
        validarContacto(req);

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new NegocioException("Ya existe un usuario con ese correo.");
        }
        if (usuarioRepository.existsByRun(run)) {
            throw new NegocioException("Ya existe un usuario con ese RUN.");
        }

        Usuario usuario = new Usuario();
        usuario.setRun(run);
        usuario.setCorreo(correo);
        usuario.setNombre(req.nombre().trim());
        usuario.setApellido(req.apellido().trim());
        usuario.setContrasena(passwordEncoder.encode(req.contrasena()));
        usuario.setTelefono(textoOpcional(req.telefono()));
        usuario.setRegion(textoOpcional(req.region()));
        usuario.setComuna(textoOpcional(req.comuna()));
        usuario.setDireccion(textoOpcional(req.direccion()));
        usuario.setRol(rol);

        return UsuarioDTO.de(usuarioRepository.save(usuario));
    }

    // ------------------------------------------------------------------
    // Consultas (ADMIN)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtener(Long id) {
        return UsuarioDTO.de(buscarPorId(id));
    }

    // ------------------------------------------------------------------
    // Edición y eliminación (ADMIN)
    // ------------------------------------------------------------------

    /**
     * @param idActual id del usuario que hace la petición (sale del token)
     */
    @Transactional
    public UsuarioDTO actualizar(Long id, UsuarioRequest req, Long idActual) {

        Usuario usuario = buscarPorId(id);

        if (req.rol() == null) {
            throw new NegocioException("El tipo de usuario es obligatorio.");
        }

        // Regla: nadie puede cambiar su propio rol
        if (usuario.getIdUsuario().equals(idActual) && req.rol() != usuario.getRol()) {
            throw new NegocioException("No puedes cambiar tu propio rol.");
        }

        String run = RunUtil.normalizar(req.run());
        String correo = normalizarCorreo(req.correo());

        validarRun(run);
        validarCorreo(correo);
        validarNombreYApellido(req);
        validarContrasena(req.contrasena(), false);
        validarContacto(req);

        if (usuarioRepository.existsByCorreoAndIdUsuarioNot(correo, id)) {
            throw new NegocioException("Ya existe un usuario con ese correo.");
        }
        if (usuarioRepository.existsByRunAndIdUsuarioNot(run, id)) {
            throw new NegocioException("Ya existe un usuario con ese RUN.");
        }

        usuario.setRun(run);
        usuario.setCorreo(correo);
        usuario.setNombre(req.nombre().trim());
        usuario.setApellido(req.apellido().trim());
        usuario.setTelefono(textoOpcional(req.telefono()));
        usuario.setRegion(textoOpcional(req.region()));
        usuario.setComuna(textoOpcional(req.comuna()));
        usuario.setDireccion(textoOpcional(req.direccion()));
        usuario.setRol(req.rol());

        // La contraseña solo cambia si viene una nueva
        if (hayTexto(req.contrasena())) {
            usuario.setContrasena(passwordEncoder.encode(req.contrasena()));
        }

        return UsuarioDTO.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public void eliminar(Long id, Long idActual) {

        Usuario usuario = buscarPorId(id);

        if (usuario.getIdUsuario().equals(idActual)) {
            throw new NegocioException("No puedes eliminar tu propia cuenta.");
        }

        try {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();   // fuerza el error aquí si tiene compras asociadas

        } catch (DataIntegrityViolationException e) {
            throw new NegocioException("No se puede eliminar: el usuario tiene compras asociadas.");
        }
    }

    // ------------------------------------------------------------------
    // Mi perfil (cualquier usuario logueado)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public UsuarioDTO perfil(Long idActual) {
        return UsuarioDTO.de(buscarPorId(idActual));
    }

    /**
     * Edita MI perfil. No se puede cambiar el RUN, el correo ni el rol
     * (el correo es la identidad de la cuenta). La contraseña es opcional.
     */
    @Transactional
    public UsuarioDTO actualizarPerfil(Long idActual, UsuarioRequest req) {

        Usuario usuario = buscarPorId(idActual);

        validarNombreYApellido(req);
        validarContrasena(req.contrasena(), false);
        validarContacto(req);

        usuario.setNombre(req.nombre().trim());
        usuario.setApellido(req.apellido().trim());
        usuario.setTelefono(textoOpcional(req.telefono()));
        usuario.setRegion(textoOpcional(req.region()));
        usuario.setComuna(textoOpcional(req.comuna()));
        usuario.setDireccion(textoOpcional(req.direccion()));

        if (hayTexto(req.contrasena())) {
            usuario.setContrasena(passwordEncoder.encode(req.contrasena()));
        }

        return UsuarioDTO.de(usuarioRepository.save(usuario));
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    private void validarRun(String run) {
        if (!RunUtil.esValido(run)) {
            throw new NegocioException(
                    "El RUN no es válido. Escríbelo sin puntos ni guion (7 a 9 caracteres).");
        }
    }

    private void validarCorreo(String correo) {

        if (correo.isEmpty()) {
            throw new NegocioException("El correo es obligatorio.");
        }
        if (correo.length() > 100) {
            throw new NegocioException("El correo no puede superar los 100 caracteres.");
        }

        if (!correo.matches("^[^\\s@]+@(duoc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$")) {
            throw new NegocioException(
                    "El correo debe ser @duoc.cl, @profesor.duoc.cl o @gmail.com.");
        }
    }

    private void validarNombreYApellido(UsuarioRequest req) {

        if (!hayTexto(req.nombre())) {
            throw new NegocioException("El nombre es obligatorio.");
        }
        if (req.nombre().trim().length() > 50) {
            throw new NegocioException("El nombre no puede superar los 50 caracteres.");
        }
        if (!hayTexto(req.apellido())) {
            throw new NegocioException("El apellido es obligatorio.");
        }
        if (req.apellido().trim().length() > 100) {
            throw new NegocioException("El apellido no puede superar los 100 caracteres.");
        }
    }

    private void validarContrasena(String contrasena, boolean obligatoria) {

        if (!hayTexto(contrasena)) {
            if (obligatoria) {
                throw new NegocioException("La contraseña es obligatoria.");
            }
            return;
        }

        if (contrasena.length() < 4 || contrasena.length() > 10) {
            throw new NegocioException("La contraseña debe tener entre 4 y 10 caracteres.");
        }
    }

    /*
     * El teléfono, la región, la comuna y la dirección son OPCIONALES:
     * una persona puede crear su cuenta sin pensar en comprar todavía.
     * El Checkout se los pedirá cuando vaya a pagar.
     * Si vienen, solo se revisa que no superen el largo de la base de datos.
     */
    private void validarContacto(UsuarioRequest req) {

        if (largo(req.telefono()) > 20) {
            throw new NegocioException("El teléfono no puede superar los 20 caracteres.");
        }
        if (largo(req.direccion()) > 300) {
            throw new NegocioException("La dirección no puede superar los 300 caracteres.");
        }
        if (largo(req.region()) > 30 || largo(req.comuna()) > 30) {
            throw new NegocioException("La región y la comuna no pueden superar los 30 caracteres.");
        }
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }

    private boolean hayTexto(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    // Devuelve el texto limpio, o null si no vino nada
    private String textoOpcional(String texto) {
        return hayTexto(texto) ? texto.trim() : null;
    }

    private int largo(String texto) {
        return texto == null ? 0 : texto.trim().length();
    }
}