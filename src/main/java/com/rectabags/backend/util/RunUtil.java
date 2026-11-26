package com.rectabags.backend.util;

/*
 * Utilidades para el RUN chileno.
 * Formato que usa el proyecto: sin puntos ni guion, de 7 a 9 caracteres,
 * con el dígito verificador al final (puede ser K). Ejemplo: 123456785
 */
public final class RunUtil {

    private RunUtil() {
    }

    /** Quita puntos, guion y espacios, y deja la K en mayúscula. */
    public static String normalizar(String run) {
        if (run == null) {
            return "";
        }
        return run.replaceAll("[.\\-\\s]", "").toUpperCase();
    }

    /** Devuelve true si el RUN (ya normalizado) cumple el largo y el módulo 11. */
    public static boolean esValido(String run) {
        if (run == null || run.length() < 7 || run.length() > 9) {
            return false;
        }

        String cuerpo = run.substring(0, run.length() - 1);
        char digito = run.charAt(run.length() - 1);

        if (!cuerpo.matches("\\d+")) {
            return false;
        }

        return calcularDigito(cuerpo) == digito;
    }

    /** Calcula el dígito verificador con el algoritmo módulo 11. */
    static char calcularDigito(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;

        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }

        int resto = 11 - (suma % 11);

        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }
}
