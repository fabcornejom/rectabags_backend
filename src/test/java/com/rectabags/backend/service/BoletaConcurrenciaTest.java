package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rectabags.backend.dto.BoletaDTO;
import com.rectabags.backend.dto.CompraRequest;
import com.rectabags.backend.dto.ItemCompraDTO;
import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.model.EstadoBoleta;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.ProductoRepository;

/**
 * Dos conexiones reales compiten por el mismo stock en PostgreSQL.
 * Las transacciones deben confirmar para observarse entre hilos; la limpieza
 * elimina exclusivamente los datos ficticios identificados por esta prueba.
 */
@SpringBootTest
@ActiveProfiles("test")
@Timeout(30)
class BoletaConcurrenciaTest {
    @Autowired private BoletaService service;
    @Autowired private ProductoRepository productos;
    @Autowired private CategoriaRepository categorias;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    private TransactionTemplate transaccion;
    private ExecutorService hilos;
    private Long idProducto;
    private Long idCategoria;
    private String correo;
    private String conexionesPrueba;

    private record Resultado(BoletaDTO boleta, RuntimeException error) { }

    @BeforeEach
    void prepararDatosFicticios() {
        transaccion = new TransactionTemplate(transactionManager);
        hilos = Executors.newFixedThreadPool(2);
        String marca = UUID.randomUUID().toString();
        correo = "concurrencia." + marca + "@gmail.com";
        conexionesPrueba = "rectabags-test-" + marca;
        transaccion.executeWithoutResult(status -> {
            Categoria categoria = categorias.save(new Categoria(null, "Concurrencia " + marca, "Prueba temporal"));
            idCategoria = categoria.getIdCategoria();
            Producto producto = new Producto();
            producto.setCategoria(categoria);
            producto.setCodigo("CON-" + marca.substring(0, 8));
            producto.setNombre("Producto ficticio de concurrencia");
            producto.setPrecio(new BigDecimal("1000.25"));
            producto.setStock(1);
            producto.setStockCritico(0);
            producto.setActivo(true);
            idProducto = productos.saveAndFlush(producto).getIdProducto();
        });
    }

    @AfterEach
    void limpiarSoloLosDatosDeEstaPrueba() throws InterruptedException {
        try {
            if (hilos != null) {
                hilos.shutdownNow();
                assertTrue(hilos.awaitTermination(15, TimeUnit.SECONDS), "Los hilos deben terminar antes de limpiar.");
            }
        } finally {
            if (idProducto != null) {
                transaccion.executeWithoutResult(status -> {
                    jdbc.update("delete from detalle_boleta where id_producto = ?", idProducto);
                    jdbc.update("delete from boleta where correo_cliente = ?", correo);
                    jdbc.update("delete from producto where id_producto = ?", idProducto);
                    jdbc.update("delete from categoria where id_categoria = ?", idCategoria);
                });
                assertEquals(0L, jdbc.queryForObject("select count(*) from boleta where correo_cliente = ?", Long.class, correo));
                assertFalse(productos.existsById(idProducto));
                assertFalse(categorias.existsById(idCategoria));
            }
        }
    }

    @Test
    void dosComprasDeLaUltimaUnidadSoloConfirmanUnaBoleta() throws Exception {
        List<Resultado> resultados = competir(() -> service.crearCompra(compra()), () -> service.crearCompra(compra()));

        assertEquals(1, resultados.stream().filter(r -> r.boleta() != null).count());
        Resultado rechazada = resultados.stream().filter(r -> r.error() != null).findFirst().orElseThrow();
        assertInstanceOf(IllegalArgumentException.class, rechazada.error());
        assertTrue(rechazada.error().getMessage().contains("Stock insuficiente"));
        assertEquals(0, stockActual());
        assertEquals(1L, cantidadBoletas());
        assertEquals(1L, jdbc.queryForObject("select count(*) from detalle_boleta where id_producto = ?", Long.class, idProducto));
    }

    @Test
    void dosCancelacionesSimultaneasDevuelvenLasUnidadesUnaSolaVez() throws Exception {
        Long idBoleta = service.crearCompra(compra()).idBoleta();
        List<Resultado> resultados = competir(
                () -> service.cambiarEstado(idBoleta, EstadoBoleta.CANCELADA),
                () -> service.cambiarEstado(idBoleta, EstadoBoleta.CANCELADA));

        for (Resultado resultado : resultados) {
            assertNull(resultado.error());
            assertEquals(EstadoBoleta.CANCELADA, resultado.boleta().estado());
        }
        assertEquals(1, stockActual());
        assertEquals(1L, cantidadBoletas());
    }

    @Test
    void comprarYCancelarOtraBoletaNoPierdeNingunMovimientoDeStock() throws Exception {
        transaccion.executeWithoutResult(status -> productos.buscarParaActualizar(idProducto).orElseThrow().setStock(2));
        Long idOriginal = service.crearCompra(compra()).idBoleta();
        List<Resultado> resultados = competir(
                () -> service.cambiarEstado(idOriginal, EstadoBoleta.CANCELADA),
                () -> service.crearCompra(compra()));

        assertTrue(resultados.stream().allMatch(r -> r.error() == null));
        assertEquals(1, stockActual(), "2 iniciales - compra original + devolución - nueva compra = 1");
        assertEquals(2L, cantidadBoletas());
        assertEquals(1L, jdbc.queryForObject("select count(*) from boleta where correo_cliente = ? and estado = 'CANCELADA'", Long.class, correo));
        assertEquals(1L, jdbc.queryForObject("select count(*) from boleta where correo_cliente = ? and estado = 'PENDIENTE'", Long.class, correo));
    }

    private List<Resultado> competir(Supplier<BoletaDTO> primera, Supplier<BoletaDTO> segunda) throws Exception {
        List<Future<Resultado>> pendientes = new ArrayList<>();
        transaccion.executeWithoutResult(status -> {
            productos.buscarParaActualizar(idProducto).orElseThrow();
            pendientes.add(hilos.submit(() -> ejecutar(primera)));
            pendientes.add(hilos.submit(() -> ejecutar(segunda)));
            // No basta con iniciar dos hilos: ambos deben estar esperando un
            // bloqueo real antes de liberar la fila y observar el resultado.
            long limite = System.nanoTime() + Duration.ofSeconds(8).toNanos();
            while (System.nanoTime() < limite) {
                jdbc.execute("select pg_stat_clear_snapshot()");
                Integer esperando = jdbc.queryForObject(
                        "select count(*) from pg_stat_activity where application_name = ? and wait_event_type = 'Lock'",
                        Integer.class, conexionesPrueba);
                if (esperando != null && esperando == 2) return;
                try { Thread.sleep(20); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Se interrumpió la coordinación de las transacciones.", e);
                }
            }
            fail("Las dos conexiones no llegaron a competir por el bloqueo esperado.");
        });
        List<Resultado> resultados = new ArrayList<>();
        for (Future<Resultado> pendiente : pendientes) resultados.add(pendiente.get(15, TimeUnit.SECONDS));
        return resultados;
    }

    private Resultado ejecutar(Supplier<BoletaDTO> operacion) {
        try {
            return new Resultado(transaccion.execute(status -> {
                jdbc.queryForObject("select set_config('application_name', ?, true)", String.class, conexionesPrueba);
                jdbc.execute("set local lock_timeout = '12s'");
                return operacion.get();
            }), null);
        } catch (RuntimeException e) { return new Resultado(null, e); }
    }

    private int stockActual() { return productos.findById(idProducto).orElseThrow().getStock(); }
    private long cantidadBoletas() {
        return jdbc.queryForObject("select count(*) from boleta where correo_cliente = ?", Long.class, correo);
    }

    private CompraRequest compra() {
        return new CompraRequest("Auditoria", "Concurrencia", correo, "912345678", "Direccion ficticia 123",
                "Región Metropolitana", "Santiago", "transferencia", List.of(new ItemCompraDTO(idProducto, 1)));
    }
}
