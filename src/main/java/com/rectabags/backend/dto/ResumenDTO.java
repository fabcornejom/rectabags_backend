package com.rectabags.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ResumenDTO(long cantidadOrdenes, BigDecimal totalVentas, long unidadesVendidas,
        BigDecimal ticketPromedio, ProductoVendido productoMasVendido,
        List<ProductoVendido> rankingProductos, Map<String, Long> comprasPorEstado) {
    public record ProductoVendido(Long idProducto, String nombre, long unidades, BigDecimal total) {
    }
}
