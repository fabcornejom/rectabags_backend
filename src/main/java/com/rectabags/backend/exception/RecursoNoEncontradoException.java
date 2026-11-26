package com.rectabags.backend.exception;

/** Cuando algo no existe en la base de datos. Responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
