package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rectabags.backend.dto.BoletaDTO;
import com.rectabags.backend.dto.CompraRequest;
import com.rectabags.backend.dto.ItemCompraDTO;
import com.rectabags.backend.model.Boleta;
import com.rectabags.backend.model.DetalleBoleta;
import com.rectabags.backend.model.EstadoBoleta;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.BoletaRepository;
import com.rectabags.backend.repository.DetalleBoletaRepository;
import com.rectabags.backend.repository.ProductoRepository;
import com.rectabags.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock private BoletaRepository boletas;
    @Mock private DetalleBoletaRepository detalles;
    @Mock private ProductoRepository productos;
    @Mock private UsuarioRepository usuarios;

    private BoletaService service;

    @BeforeEach
    void preparar() {
        service = new BoletaService(boletas, detalles, productos, usuarios);
    }

    @Test
    void compraCorrectaCalculaTotalesDescuentaStockYGuardaDetallesHistoricos() {
        Producto mochila = producto(1L, "Mochila", "10000.50", 5);
        Producto bolso = producto(2L, "Bolso", "5000.25", 3);
        when(productos.buscarParaActualizar(1L)).thenReturn(Optional.of(mochila));
        when(productos.buscarParaActualizar(2L)).thenReturn(Optional.of(bolso));
        simularGuardado();

        BoletaDTO respuesta = service.crearCompra(compra(
                new ItemCompraDTO(1L, 2), new ItemCompraDTO(2L, 1)));

        assertEquals(3, mochila.getStock());
        assertEquals(2, bolso.getStock());
        assertEquals(new BigDecimal("25001.25"), respuesta.total());
        assertEquals(respuesta.total(), respuesta.subtotal());
        assertEquals(2, respuesta.detalles().size());
        assertEquals(new BigDecimal("20001.00"), respuesta.detalles().get(0).subTotal());
        assertEquals(EstadoBoleta.PENDIENTE, respuesta.estado());
        assertNull(respuesta.idUsuario());
        assertNotNull(respuesta.fechaEmision());
        assertNull(respuesta.fechaCierre());
        assertTrue(respuesta.folio().startsWith("RB-"));
        assertEquals(40, respuesta.folio().length());

        ArgumentCaptor<Boleta> cabecera = ArgumentCaptor.forClass(Boleta.class);
        verify(boletas).save(cabecera.capture());
        assertNull(cabecera.getValue().getUsuario());
        assertEquals("Marco", cabecera.getValue().getNombreCliente());

        ArgumentCaptor<List<DetalleBoleta>> lineas = ArgumentCaptor.forClass(List.class);
        verify(detalles).saveAll(lineas.capture());
        assertSame(cabecera.getValue(), lineas.getValue().get(0).getBoleta());
        mochila.setNombre("Nombre posterior");
        mochila.setPrecio(new BigDecimal("99999"));
        assertEquals("Mochila", lineas.getValue().get(0).getNombreProducto());
        assertEquals(new BigDecimal("10000.50"), lineas.getValue().get(0).getPrecioUnidad());
    }

    @Test
    void faltaStockEnSegundoProductoNoGuardaNiModificaElPrimero() {
        Producto primero = producto(1L, "Mochila", "10000", 5);
        Producto segundo = producto(2L, "Bolso", "5000", 0);
        when(productos.buscarParaActualizar(1L)).thenReturn(Optional.of(primero));
        when(productos.buscarParaActualizar(2L)).thenReturn(Optional.of(segundo));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crearCompra(compra(new ItemCompraDTO(1L, 2), new ItemCompraDTO(2L, 1))));

        assertTrue(error.getMessage().contains("Bolso"));
        assertEquals(5, primero.getStock());
        assertEquals(0, segundo.getStock());
        verifyNoInteractions(boletas, detalles);
        verify(productos, never()).saveAll(any());
    }

    @Test
    void usaPrecioOfertaDeLaBaseDeDatos() {
        Producto producto = producto(1L, "Mochila", "10000.50", 5);
        producto.setPrecioOferta(new BigDecimal("7990.25"));
        when(productos.buscarParaActualizar(1L)).thenReturn(Optional.of(producto));
        simularGuardado();

        BoletaDTO respuesta = service.crearCompra(compra(new ItemCompraDTO(1L, 2)));

        assertEquals(new BigDecimal("15980.50"), respuesta.total());
        assertEquals(new BigDecimal("7990.25"), respuesta.detalles().get(0).precioUnidad());
    }

    @Test
    void productoInactivoEsRechazadoSinGuardar() {
        Producto producto = producto(1L, "Mochila", "10000", 5);
        producto.setActivo(false);
        when(productos.buscarParaActualizar(1L)).thenReturn(Optional.of(producto));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crearCompra(compra(new ItemCompraDTO(1L, 1))));

        assertTrue(error.getMessage().contains("Mochila"));
        assertEquals(5, producto.getStock());
        verifyNoInteractions(boletas, detalles);
    }

    @Test
    void productoInexistenteEsRechazadoSinGuardar() {
        when(productos.buscarParaActualizar(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.crearCompra(compra(new ItemCompraDTO(9L, 1))));

        verifyNoInteractions(boletas, detalles);
    }

    @Test
    void itemsRepetidosCompruebanElStockDeLaCantidadTotal() {
        Producto producto = producto(1L, "Mochila", "10000", 3);
        when(productos.buscarParaActualizar(1L)).thenReturn(Optional.of(producto));

        assertThrows(IllegalArgumentException.class, () -> service.crearCompra(compra(
                new ItemCompraDTO(1L, 2), new ItemCompraDTO(1L, 2))));

        assertEquals(3, producto.getStock());
        verifyNoInteractions(boletas, detalles);
    }

    @Test
    void compraVaciaEsRechazadaSinAccederALaBase() {
        assertThrows(IllegalArgumentException.class, () -> service.crearCompra(compra()));
        verifyNoInteractions(boletas, detalles, productos);
    }

    @Test
    void cantidadNoPositivaEsRechazadaSinAccederALaBase() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCompra(compra(new ItemCompraDTO(1L, 0))));
        verifyNoInteractions(boletas, detalles, productos);
    }

    private void simularGuardado() {
        when(boletas.save(any(Boleta.class))).thenAnswer(invocacion -> {
            Boleta boleta = invocacion.getArgument(0);
            boleta.setIdBoleta(10L);
            return boleta;
        });
        when(detalles.saveAll(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void cancelarCompraPagadaDevuelveStockUnaSolaVezYConservaPreciosHistoricos() {
        Boleta boleta = new Boleta();
        boleta.setIdBoleta(10L);
        boleta.setEstado(EstadoBoleta.PAGADA);
        Producto producto = producto(1L, "Nombre actual", "2000", 3);
        DetalleBoleta detalle = new DetalleBoleta();
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setNombreProducto("Nombre historico");
        detalle.setPrecioUnidad(new BigDecimal("1000"));
        detalle.setSubTotal(new BigDecimal("2000"));
        when(boletas.buscarParaActualizar(10L)).thenReturn(Optional.of(boleta));
        when(detalles.productosDeBoleta(10L)).thenReturn(List.of(1L));
        when(productos.buscarParaActualizar(1L)).thenReturn(Optional.of(producto));
        when(detalles.findByBoletaIdBoletaOrderByIdDetalleBoletaAsc(10L)).thenReturn(List.of(detalle));

        BoletaDTO respuesta = service.cambiarEstado(10L, EstadoBoleta.CANCELADA);
        service.cambiarEstado(10L, EstadoBoleta.CANCELADA);

        assertEquals(5, producto.getStock());
        assertEquals(EstadoBoleta.CANCELADA, respuesta.estado());
        assertEquals("Nombre historico", respuesta.detalles().get(0).nombreProducto());
        assertEquals(new BigDecimal("1000"), respuesta.detalles().get(0).precioUnidad());
        verify(productos, times(1)).saveAll(anyList());
        verify(boletas, times(1)).save(boleta);
    }

    @Test
    void boletaCerradaNoPermiteCambiarEstadoNiModificarStock() {
        Boleta boleta = new Boleta();
        boleta.setEstado(EstadoBoleta.PENDIENTE);
        boleta.setFechaCierre(java.time.LocalDateTime.now());
        when(boletas.buscarParaActualizar(10L)).thenReturn(Optional.of(boleta));
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(10L, EstadoBoleta.CANCELADA));
        verifyNoInteractions(productos, detalles);
        verify(boletas, never()).save(any());
    }

    private Producto producto(Long id, String nombre, String precio, int stock) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setStock(stock);
        producto.setActivo(true);
        return producto;
    }

    private CompraRequest compra(ItemCompraDTO... items) {
        return new CompraRequest(" Marco ", "Carrasco", "cliente@gmail.com", "912345678",
                "Av. Principal 123", "Región Metropolitana", "Santiago", "transferencia", List.of(items));
    }
}
