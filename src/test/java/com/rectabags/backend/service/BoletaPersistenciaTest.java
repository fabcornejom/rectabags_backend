package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.rectabags.backend.dto.BoletaDTO;
import com.rectabags.backend.dto.CompraRequest;
import com.rectabags.backend.dto.ItemCompraDTO;
import com.rectabags.backend.model.Boleta;
import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.model.DetalleBoleta;
import com.rectabags.backend.model.EstadoBoleta;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.BoletaRepository;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.DetalleBoletaRepository;
import com.rectabags.backend.repository.ProductoRepository;

import jakarta.persistence.EntityManager;

/**
 * Pruebas sobre la base PostgreSQL configurada para el proyecto.
 * Cada prueba revierte su transaccion, incluidos sus datos ficticios.
 */
@SpringBootTest
@Transactional
class BoletaPersistenciaTest {

    @Autowired private BoletaService service;
    @Autowired private BoletaRepository boletas;
    @Autowired private ProductoRepository productos;
    @Autowired private CategoriaRepository categorias;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private DetalleBoletaRepository detalles;

    private long boletasIniciales;
    private long detallesIniciales;
    private long productosIniciales;
    private long categoriasIniciales;

    @BeforeTransaction
    void registrarCantidadesIniciales() {
        boletasIniciales = boletas.count();
        detallesIniciales = detalles.count();
        productosIniciales = productos.count();
        categoriasIniciales = categorias.count();
    }

    @AfterTransaction
    void verificarQueNoQuedenRegistrosDePrueba() {
        assertEquals(boletasIniciales, boletas.count(), "La prueba no debe dejar boletas.");
        assertEquals(detallesIniciales, detalles.count(), "La prueba no debe dejar detalles.");
        assertEquals(productosIniciales, productos.count(), "La prueba no debe dejar productos.");
        assertEquals(categoriasIniciales, categorias.count(), "La prueba no debe dejar categorias.");
    }

    @Test
    void compraGuardaCabeceraDetallesYStockEnPostgresConPrecioOferta() {
        Producto mochila = crearProducto("Mochila prueba", "10000.50", "7990.25", 5);
        Producto bolso = crearProducto("Bolso prueba", "5000.25", null, 3);

        BoletaDTO respuesta = service.crearCompra(compra(
                new ItemCompraDTO(mochila.getIdProducto(), 2),
                new ItemCompraDTO(bolso.getIdProducto(), 1)));
        // flush envia los SQL pendientes; clear impide verificar solo objetos en memoria.
        entityManager.flush();
        entityManager.clear();

        Boleta guardada = boletas.findById(respuesta.idBoleta()).orElseThrow();
        assertEquals(new BigDecimal("20980.75"), guardada.getTotal());
        assertEquals(guardada.getTotal(), guardada.getSubtotal());
        assertEquals(EstadoBoleta.PENDIENTE, guardada.getEstado());
        assertNull(guardada.getUsuario());
        assertNotNull(guardada.getFechaEmision());
        assertEquals(respuesta.folio(), guardada.getFolio());
        assertEquals(3, productos.findById(mochila.getIdProducto()).orElseThrow().getStock());
        assertEquals(2, productos.findById(bolso.getIdProducto()).orElseThrow().getStock());
        assertEquals(boletasIniciales + 1, boletas.count());
        assertEquals(detallesIniciales + 2, detalles.count());

        DetalleBoleta detalle = detalles.findById(respuesta.detalles().get(0).idDetalleBoleta()).orElseThrow();
        assertEquals(guardada.getIdBoleta(), detalle.getBoleta().getIdBoleta());
        assertEquals(mochila.getIdProducto(), detalle.getProducto().getIdProducto());
        assertEquals(new BigDecimal("7990.25"), detalle.getPrecioUnidad());
        assertEquals(new BigDecimal("15980.50"), detalle.getSubTotal());
        assertEquals(2, detalle.getCantidad());
    }

    @Test
    void faltaStockNoGuardaCompraNiDescuentaOtrosProductos() {
        Producto mochila = crearProducto("Mochila prueba", "10000", null, 5);
        Producto bolso = crearProducto("Bolso agotado", "5000", null, 0);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crearCompra(compra(new ItemCompraDTO(mochila.getIdProducto(), 2),
                        new ItemCompraDTO(bolso.getIdProducto(), 1))));
        assertTrue(error.getMessage().contains("Bolso agotado"));
        entityManager.clear();

        assertEquals(boletasIniciales, boletas.count());
        assertEquals(detallesIniciales, detalles.count());
        assertEquals(5, productos.findById(mochila.getIdProducto()).orElseThrow().getStock());
        assertEquals(0, productos.findById(bolso.getIdProducto()).orElseThrow().getStock());
    }

    @Test
    void detalleConservaNombreYPrecioAunqueCambieElProducto() {
        Producto producto = crearProducto("Nombre original", "10000.50", null, 5);
        BoletaDTO respuesta = service.crearCompra(compra(new ItemCompraDTO(producto.getIdProducto(), 1)));
        producto.setNombre("Nombre modificado");
        producto.setPrecio(new BigDecimal("15000.75"));
        entityManager.flush();
        entityManager.clear();

        DetalleBoleta detalle = detalles.findById(respuesta.detalles().get(0).idDetalleBoleta()).orElseThrow();
        assertEquals("Nombre original", detalle.getNombreProducto());
        assertEquals(new BigDecimal("10000.50"), detalle.getPrecioUnidad());
        assertEquals(new BigDecimal("10000.50"), detalle.getBoleta().getTotal());
        assertEquals("Nombre modificado", detalle.getProducto().getNombre());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void errorDespuesDeEnviarCabeceraYStockALaBaseRevierteLaTransaccion() {
        registrarCantidadesIniciales();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // No se utiliza la reversion automatica del test: se intenta finalizar la transaccion.
        assertThrows(UnexpectedRollbackException.class, () -> transaction.executeWithoutResult(status -> {
            try {
                Producto producto = crearProducto("Producto rollback", "10000.50", null, 5);
                doAnswer(invocacion -> {
                    entityManager.flush();
                    // Confirma por SQL que ya se escribieron cabecera y stock en PostgreSQL.
                    assertEquals(boletasIniciales + 1,
                            ((Number) entityManager.createNativeQuery("SELECT count(*) FROM boleta")
                                    .getSingleResult()).longValue());
                    assertEquals(3, ((Number) entityManager.createNativeQuery(
                            "SELECT stock FROM producto WHERE id_producto = :id")
                            .setParameter("id", producto.getIdProducto()).getSingleResult()).intValue());
                    throw new IllegalStateException("Fallo controlado al guardar detalles.");
                }).when(detalles).saveAll(anyList());

                IllegalStateException error = assertThrows(IllegalStateException.class,
                        () -> service.crearCompra(compra(new ItemCompraDTO(producto.getIdProducto(), 2))));
                assertEquals("Fallo controlado al guardar detalles.", error.getMessage());
                assertTrue(status.isRollbackOnly(), "El fallo debe marcar la transaccion para revertirse.");
            } catch (RuntimeException | Error error) {
                status.setRollbackOnly();
                throw error;
            }
        }));
        verificarQueNoQuedenRegistrosDePrueba();
    }

    private Producto crearProducto(String nombre, String precio, String oferta, int stock) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Prueba-" + UUID.randomUUID());
        categoria.setDescripcion("Datos ficticios de prueba transaccional.");
        categorias.save(categoria);

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setCodigo("TEST-" + UUID.randomUUID().toString().substring(0, 8));
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setPrecioOferta(oferta == null ? null : new BigDecimal(oferta));
        producto.setStock(stock);
        producto.setStockCritico(0);
        producto.setActivo(true);
        return productos.saveAndFlush(producto);
    }

    private CompraRequest compra(ItemCompraDTO... items) {
        return new CompraRequest("Cliente", "Prueba", "prueba@gmail.com", "912345678",
                "Direccion ficticia 123", "Región Metropolitana", "Santiago", "transferencia", List.of(items));
    }
}
