package com.rectabags.backend.exception;

/** Error de regla de negocio o datos inválidos. Responde 400. */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
