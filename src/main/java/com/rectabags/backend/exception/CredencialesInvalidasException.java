package com.rectabags.backend.exception;

/** Correo o contraseña incorrectos en el login. Responde 401. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos.");
    }
}
