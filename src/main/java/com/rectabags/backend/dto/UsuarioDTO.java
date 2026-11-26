package com.rectabags.backend.dto;

import com.rectabags.backend.model.Rol;
import com.rectabags.backend.model.Usuario;

/*
 * Lo que el backend devuelve de un usuario.
 * NUNCA incluye la contraseña.
 */
public record UsuarioDTO(
        Long id,
        String run,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        String region,
        String comuna,
        String direccion,
        Rol rol) {

    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(
                u.getIdUsuario(),
                u.getRun(),
                u.getNombre(),
                u.getApellido(),
                u.getCorreo(),
                u.getTelefono(),
                u.getRegion(),
                u.getComuna(),
                u.getDireccion(),
                u.getRol());
    }
}
