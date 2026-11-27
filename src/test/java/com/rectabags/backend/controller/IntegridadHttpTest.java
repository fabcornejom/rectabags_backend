package com.rectabags.backend.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.ProductoRepository;
import com.rectabags.backend.repository.UsuarioRepository;
import com.rectabags.backend.security.JwtUtil;
import com.rectabags.backend.util.RunUtil;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntegridadHttpTest {
    @Autowired private WebApplicationContext context;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CategoriaRepository categorias;
    @Autowired private ProductoRepository productos;
    @Autowired private JwtUtil jwt;
    @Autowired private JdbcTemplate jdbc;
    private MockMvc mvc;
    private String correoRegistro;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        correoRegistro = "registro." + UUID.randomUUID() + "@gmail.com";
    }

    @AfterTransaction
    void registroDePruebaNoQuedaGuardado() {
        assertFalse(usuarios.existsByCorreo(correoRegistro));
    }

    @Test
    void registroPublicoPermiteOmitirLaUbicacionOpcional() throws Exception {
        String cuerpo;
        String run;
        do {
            cuerpo = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 30_000_000));
            run = null;
            for (char digito : "0123456789K".toCharArray()) {
                if (RunUtil.esValido(cuerpo + digito)) { run = cuerpo + digito; break; }
            }
        } while (run == null || usuarios.existsByRun(run));
        String json = """
                {"run":"%s","nombre":"Registro","apellido":"Prueba","correo":"%s",
                 "contrasena":"Prueba123!","telefono":"912345678"}
                """.formatted(run, correoRegistro);

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.region").doesNotExist()).andExpect(jsonPath("$.comuna").doesNotExist())
                .andExpect(jsonPath("$.direccion").doesNotExist());
        var guardado = usuarios.findByCorreo(correoRegistro).orElseThrow();
        assertNull(guardado.getRegion());
        assertNull(guardado.getComuna());
        assertNull(guardado.getDireccion());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void categoriaConProductoActivoOInactivoResponde400YConservaLaSesion(boolean activo) throws Exception {
        Categoria categoria = categorias.save(new Categoria(null, "Integridad " + UUID.randomUUID(), "Prueba temporal"));
        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setCodigo("INT-" + UUID.randomUUID().toString().substring(0, 8));
        producto.setNombre("Producto ficticio de integridad");
        producto.setPrecio(new BigDecimal("1000"));
        producto.setStock(1);
        producto.setStockCritico(0);
        producto.setActivo(activo);
        productos.saveAndFlush(producto);
        String token = tokenAdmin();

        mvc.perform(delete("/api/v1/categorias/" + categoria.getIdCategoria()).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar: la categoría tiene productos asociados, incluidos los inactivos."));
        assertTrue(categorias.existsById(categoria.getIdCategoria()));
        assertTrue(productos.existsById(producto.getIdProducto()));
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", token)).andExpect(status().isOk());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void limiteNumericoDePostgresResponde400YElTokenSigueValido() throws Exception {
        Categoria categoria = categorias.save(new Categoria(null, "Limite " + UUID.randomUUID(), "Prueba temporal"));
        String codigo = "LIM-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            String token = tokenAdmin();
            String json = """
                    {"codigo":"%s","nombre":"Producto de prueba","precio":100000000.00,
                     "stock":1,"stockCritico":0,"idCategoria":%d}
                    """.formatted(codigo, categoria.getIdCategoria());
            mvc.perform(post("/api/v1/productos").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Los datos no cumplen los límites o las relaciones permitidas. Revisa los valores e inténtalo nuevamente."));
            mvc.perform(get("/api/v1/usuarios/me").header("Authorization", token)).andExpect(status().isOk());
            assertFalse(productos.existsByCodigoIgnoreCase(codigo));
        } finally {
            // Esta prueba necesita terminar la transacción fallida antes de
            // reutilizar el JWT; limpiar solo su categoría y eventual producto.
            jdbc.update("delete from producto where codigo = ?", codigo);
            categorias.deleteById(categoria.getIdCategoria());
            assertFalse(productos.existsByCodigoIgnoreCase(codigo));
            assertFalse(categorias.existsById(categoria.getIdCategoria()));
        }
    }

    @Test
    void despachoInternoDeErrorConserva500YNoAbreLaRutaPublica() throws Exception {
        mvc.perform(get("/error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                })
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/prueba-interna"))
                .andExpect(status().isInternalServerError());
        mvc.perform(get("/error")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
    }

    private String tokenAdmin() {
        return "Bearer " + jwt.generarToken(usuarios.findByCorreo("admin@duoc.cl").orElseThrow());
    }
}
