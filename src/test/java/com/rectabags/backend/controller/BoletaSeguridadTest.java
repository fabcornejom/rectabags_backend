package com.rectabags.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.rectabags.backend.dto.CompraRequest;
import com.rectabags.backend.dto.ItemCompraDTO;
import com.rectabags.backend.model.*;
import com.rectabags.backend.repository.*;
import com.rectabags.backend.security.JwtUtil;
import com.rectabags.backend.service.BoletaService;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@Transactional
class BoletaSeguridadTest {
    @Autowired private WebApplicationContext context;
    @Autowired private JwtUtil jwt;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CategoriaRepository categorias;
    @Autowired private ProductoRepository productos;
    @Autowired private BoletaService service;
    private MockMvc mvc;
    private Usuario cliente;
    private Usuario admin;
    private Usuario vendedor;
    private Producto producto;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        cliente = usuarios.findByCorreo("cliente@gmail.com").orElseThrow();
        admin = usuarios.findByCorreo("admin@duoc.cl").orElseThrow();
        vendedor = usuarios.findByCorreo("vendedor@duoc.cl").orElseThrow();
        Categoria categoria = new Categoria(null, "Test-" + UUID.randomUUID(), "Prueba reversible");
        categorias.save(categoria);
        producto = new Producto();
        producto.setCategoria(categoria);
        producto.setCodigo("TEST-" + UUID.randomUUID().toString().substring(0, 8));
        producto.setNombre("Producto prueba permisos");
        producto.setPrecio(new BigDecimal("1000.50"));
        producto.setStock(5);
        producto.setStockCritico(0);
        productos.saveAndFlush(producto);
    }

    private String token(Usuario usuario) { return "Bearer " + jwt.generarToken(usuario); }

    private Long compra(Long idUsuario) {
        return service.crearCompra(new CompraRequest("Cliente", "Prueba", "prueba@gmail.com",
                "912345678", "Direccion ficticia 123", "Región Metropolitana", "Santiago", "transferencia",
                List.of(new ItemCompraDTO(producto.getIdProducto(), 1))), idUsuario).idBoleta();
    }

    @Test
    void listadoSinTokenResponde401() throws Exception {
        mvc.perform(get("/api/v1/boletas")).andExpect(status().isUnauthorized());
    }

    @Test
    void soloAdminCambiaEstadoYCancelarDosVecesNoDuplicaStock() throws Exception {
        Long id = compra(cliente.getIdUsuario());
        String ruta = "/api/v1/boletas/" + id + "/estado";
        for (Usuario usuario : List.of(cliente, vendedor)) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(ruta)
                    .header("Authorization", token(usuario)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estado\":\"CANCELADA\"}")).andExpect(status().isForbidden());
        }
        for (int intento = 0; intento < 2; intento++) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(ruta)
                    .header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estado\":\"CANCELADA\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
            org.junit.jupiter.api.Assertions.assertEquals(5, productos.findById(producto.getIdProducto()).orElseThrow().getStock());
        }
    }

    @Test
    void validaEstadosYRechazaSaltosYReactivaciones() throws Exception {
        Long id = compra(null);
        String ruta = "/api/v1/boletas/" + id + "/estado";
        for (String json : List.of("{}", "{\"estado\":\"INVENTADO\"}", "{\"estado\":\"ENTREGADA\"}")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(ruta)
                    .header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").isNotEmpty());
        }
        for (String estado : List.of("PAGADA", "ENVIADA", "ENTREGADA")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(ruta)
                    .header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estado\":\"" + estado + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(estado));
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(ruta)
                .header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"CANCELADA\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void resumenYCierreSonSoloAdminYCierrePreservaHistorial() throws Exception {
        Long id = compra(cliente.getIdUsuario());
        for (Usuario usuario : List.of(cliente, vendedor)) {
            mvc.perform(get("/api/v1/reportes/resumen").header("Authorization", token(usuario)))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/reportes/cierre").header("Authorization", token(usuario)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/reportes/resumen").header("Authorization", token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cantidadOrdenes").value(1))
                .andExpect(jsonPath("$.totalVentas").value(1000.50));
        mvc.perform(post("/api/v1/reportes/cierre").header("Authorization", token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cantidadCerradas").value(1));
        mvc.perform(post("/api/v1/reportes/cierre").header("Authorization", token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cantidadCerradas").value(0));
        mvc.perform(get("/api/v1/reportes/resumen").header("Authorization", token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cantidadOrdenes").value(0));
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", token(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].fechaCierre").isNotEmpty());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/boletas/" + id + "/estado")
                .header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"PAGADA\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void clienteNoPuedeListarTodasLasOrdenes() throws Exception {
        mvc.perform(get("/api/v1/boletas").header("Authorization", token(cliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminYVendedorPuedenListarOrdenes() throws Exception {
        Long id = compra(cliente.getIdUsuario());
        for (Usuario usuario : List.of(admin, vendedor)) {
            mvc.perform(get("/api/v1/boletas").header("Authorization", token(usuario)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].idBoleta").value(id));
        }
    }

    @Test
    void clientePuedeVerSuBoleta() throws Exception {
        Long id = compra(cliente.getIdUsuario());
        mvc.perform(get("/api/v1/boletas/" + id).header("Authorization", token(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idUsuario").value(cliente.getIdUsuario()))
                .andExpect(jsonPath("$.detalles[0].nombreProducto").value(producto.getNombre()));
    }

    @Test
    void clienteNoPuedeVerBoletaAjenaNiDeInvitado() throws Exception {
        for (Long id : List.of(compra(admin.getIdUsuario()), compra(null))) {
            mvc.perform(get("/api/v1/boletas/" + id).header("Authorization", token(cliente)))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.mensaje").isNotEmpty());
        }
    }

    @Test
    void adminYVendedorPuedenVerBoletaDeInvitado() throws Exception {
        Long id = compra(null);
        for (Usuario usuario : List.of(admin, vendedor)) {
            mvc.perform(get("/api/v1/boletas/" + id).header("Authorization", token(usuario)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void boletaInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/v1/boletas/9223372036854775807").header("Authorization", token(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void misComprasIncluyeSoloLasDelToken() throws Exception {
        Long propia = compra(cliente.getIdUsuario());
        compra(admin.getIdUsuario());
        compra(null);
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", token(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idBoleta").value(propia));
    }

    @Test
    void historialDeOtroUsuarioEsSoloParaAdmin() throws Exception {
        Long id = compra(cliente.getIdUsuario());
        String ruta = "/api/v1/boletas/usuario/" + cliente.getIdUsuario();
        mvc.perform(get(ruta).header("Authorization", token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].idBoleta").value(id));
        for (Usuario usuario : List.of(cliente, vendedor)) {
            mvc.perform(get(ruta).header("Authorization", token(usuario))).andExpect(status().isForbidden());
        }
    }

    @Test
    void compraDesdeHttpAsociaElUsuarioDelJwt() throws Exception {
        String json = """
                {"nombreCliente":"Cliente","apellidoCliente":"Prueba","correoCliente":"prueba@gmail.com",
                 "telefonoCliente":"912345678","direccionCliente":"Direccion ficticia 123",
                 "region":"Región Metropolitana","comuna":"Santiago","metodoPago":"transferencia",
                 "items":[{"idProducto":%d,"cantidad":1}]}
                """.formatted(producto.getIdProducto());
        mvc.perform(post("/api/v1/boletas").header("Authorization", token(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idUsuario").value(cliente.getIdUsuario()));
        mvc.perform(post("/api/v1/boletas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idUsuario").doesNotExist());
    }
}
