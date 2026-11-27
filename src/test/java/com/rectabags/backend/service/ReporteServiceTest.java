package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.rectabags.backend.dto.BoletaDTO;
import com.rectabags.backend.dto.ResumenDTO;
import com.rectabags.backend.model.EstadoBoleta;
import com.rectabags.backend.repository.BoletaRepository;

class ReporteServiceTest {
    private final BoletaService boletas = mock(BoletaService.class);
    private final BoletaRepository repositorio = mock(BoletaRepository.class);
    private final ReporteService reportes = new ReporteService(boletas, repositorio);

    @Test
    void periodoVacioTieneTotalesCeroYNoTieneProductoMasVendido() {
        when(boletas.listar()).thenReturn(List.of());
        ResumenDTO resumen = reportes.resumen();
        assertEquals(0, resumen.cantidadOrdenes());
        assertEquals(BigDecimal.ZERO, resumen.totalVentas());
        assertEquals(BigDecimal.ZERO, resumen.ticketPromedio());
        assertNull(resumen.productoMasVendido());
        assertTrue(resumen.rankingProductos().isEmpty());
        assertTrue(resumen.comprasPorEstado().isEmpty());
    }

    @Test
    void cierreIncluyeTodosLosEstadosYNoReescribeFechasAnteriores() {
        List<com.rectabags.backend.model.Boleta> abiertas = java.util.Arrays.stream(EstadoBoleta.values())
                .map(estado -> {
                    com.rectabags.backend.model.Boleta boleta = new com.rectabags.backend.model.Boleta();
                    boleta.setEstado(estado);
                    return boleta;
                }).toList();
        com.rectabags.backend.model.Boleta cerrada = new com.rectabags.backend.model.Boleta();
        LocalDateTime fechaAnterior = LocalDateTime.now().minusDays(1);
        cerrada.setFechaCierre(fechaAnterior);
        when(repositorio.abiertasParaCerrar()).thenReturn(abiertas, List.of());
        assertEquals(5, reportes.cerrarPeriodo());
        assertNotNull(abiertas.get(0).getFechaCierre());
        assertTrue(abiertas.stream().allMatch(boleta -> boleta.getFechaCierre().equals(abiertas.get(0).getFechaCierre())));
        assertEquals(fechaAnterior, cerrada.getFechaCierre());
        assertEquals(0, reportes.cerrarPeriodo());
        verify(repositorio).saveAll(abiertas);
        verify(repositorio).saveAll(List.of());
    }

    @Test
    void excluyeCanceladasYCerradasYConservaCanceladasEnConteoDeEstados() {
        when(boletas.listar()).thenReturn(List.of(
                boleta("1000.25", EstadoBoleta.PAGADA, null, detalle(1L, "Mochila", 2, "1000.25")),
                boleta("2000.75", EstadoBoleta.PENDIENTE, null, detalle(2L, "Bolso", 3, "2000.75")),
                boleta("9000", EstadoBoleta.CANCELADA, null, detalle(3L, "Cancelado", 20, "9000")),
                boleta("8000", EstadoBoleta.ENTREGADA, LocalDateTime.now(), detalle(4L, "Cerrado", 30, "8000"))));
        ResumenDTO resumen = reportes.resumen();
        assertEquals(2, resumen.cantidadOrdenes());
        assertEquals(new BigDecimal("3001.00"), resumen.totalVentas());
        assertEquals(new BigDecimal("1501"), resumen.ticketPromedio());
        assertEquals(5, resumen.unidadesVendidas());
        assertEquals("Bolso", resumen.productoMasVendido().nombre());
        assertEquals(3, resumen.productoMasVendido().unidades());
        assertEquals(2, resumen.rankingProductos().size());
        assertEquals(1L, resumen.comprasPorEstado().get("CANCELADA"));
        assertFalse(resumen.comprasPorEstado().containsKey("ENTREGADA"));
    }

    @Test
    void agrupaPorIdYUsaImportesHistoricosDeLosDetalles() {
        when(boletas.listar()).thenReturn(List.of(
                boleta("1200", EstadoBoleta.ENTREGADA, null, detalle(1L, "Nombre nuevo", 2, "1200")),
                boleta("500", EstadoBoleta.PAGADA, null, detalle(1L, "Nombre anterior", 1, "500"))));
        ResumenDTO resumen = reportes.resumen();
        assertEquals(1, resumen.rankingProductos().size());
        assertEquals(3, resumen.productoMasVendido().unidades());
        assertEquals(new BigDecimal("1700"), resumen.productoMasVendido().total());
        assertEquals(new BigDecimal("850"), resumen.ticketPromedio());
    }

    private BoletaDTO.DetalleDTO detalle(Long id, String nombre, int cantidad, String total) {
        return new BoletaDTO.DetalleDTO(id, id, nombre, BigDecimal.ONE, cantidad, new BigDecimal(total));
    }

    private BoletaDTO boleta(String total, EstadoBoleta estado, LocalDateTime cierre, BoletaDTO.DetalleDTO detalle) {
        return new BoletaDTO(1L, "RB-PRUEBA", null, "Cliente", "Prueba", "prueba@gmail.com", "912345678",
                "Direccion", "Región Metropolitana", "Santiago", "transferencia", new BigDecimal(total),
                new BigDecimal(total), estado, LocalDateTime.now(), cierre, List.of(detalle));
    }
}
