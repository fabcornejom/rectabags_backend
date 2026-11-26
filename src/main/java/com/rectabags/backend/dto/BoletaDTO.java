package com.rectabags.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.rectabags.backend.model.EstadoBoleta;

public record BoletaDTO(
        Long idBoleta,
        String folio,
        Long idUsuario,
        String nombreCliente,
        String apellidoCliente,
        String correoCliente,
        String telefonoCliente,
        String direccionCliente,
        String region,
        String comuna,
        String metodoPago,
        BigDecimal subtotal,
        BigDecimal total,
        EstadoBoleta estado,
        LocalDateTime fechaEmision,
        LocalDateTime fechaCierre,
        List<DetalleDTO> detalles) {

    public record DetalleDTO(
            Long idDetalleBoleta,
            Long idProducto,
            String nombreProducto,
            BigDecimal precioUnidad,
            Integer cantidad,
            BigDecimal subTotal) {
    }
}
