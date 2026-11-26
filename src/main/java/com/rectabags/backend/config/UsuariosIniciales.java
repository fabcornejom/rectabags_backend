package com.rectabags.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.rectabags.backend.model.Rol;
import com.rectabags.backend.model.Usuario;
import com.rectabags.backend.repository.UsuarioRepository;

/*
 * Crea los 3 usuarios de prueba al arrancar la aplicación.
 * Si el correo ya existe, no hace nada (se puede reiniciar sin duplicar).
 * Las contraseñas se guardan encriptadas con BCrypt.
 */
@Component
public class UsuariosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuariosIniciales(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        crear("111111111", "Admin", "RectaBags", "admin@duoc.cl",
                "Admin123!", Rol.ADMIN);

        crear("123456785", "Vendedor", "RectaBags", "vendedor@duoc.cl",
                "Vend123!", Rol.VENDEDOR);

        crear("201234565", "Cliente", "Demo", "cliente@gmail.com",
                "Cli123!", Rol.CLIENTE);
    }

    private void crear(String run, String nombre, String apellido,
                       String correo, String contrasena, Rol rol) {

        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }

        Usuario u = new Usuario();
        u.setRun(run);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCorreo(correo);
        u.setContrasena(passwordEncoder.encode(contrasena));
        u.setTelefono("912345678");
        u.setRegion("Región Metropolitana");
        u.setComuna("Santiago");
        u.setDireccion("Av. Providencia 1234");
        u.setRol(rol);

        usuarioRepository.save(u);
    }
}
