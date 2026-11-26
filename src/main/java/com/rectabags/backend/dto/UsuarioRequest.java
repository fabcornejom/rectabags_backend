package com.rectabags.backend.dto;

import com.rectabags.backend.model.Rol;

/*
 * Datos que envía el frontend para registrar, crear o editar un usuario.
 * No todos los campos se usan siempre:
 *  - registro:      ignora "rol" (siempre CLIENTE)
 *  - /usuarios/me:  ignora "run", "correo" y "rol"
 *  - admin:         usa todos
 */
public record UsuarioRequest(
        String run,
        String nombre,
        String apellido,
        String correo,
        String contrasena,
        String telefono,
        String region,
        String comuna,
        String direccion,
        Rol rol) {
}
