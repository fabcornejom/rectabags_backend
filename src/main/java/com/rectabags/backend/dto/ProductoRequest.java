package com.rectabags.backend.dto;

import java.math.BigDecimal;

/*
 * Datos que envía el frontend para crear o editar un producto.
 */
public record ProductoRequest(
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal precioOferta,
        Integer stock,
        Integer stockCritico,
        String imagen,
        Long idCategoria
) {
}