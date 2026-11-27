package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rectabags.backend.dto.ProductoRequest;
import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProductoService productoService;


    // Producto de ejemplo con datos correctos
    private ProductoRequest requestValido() {
        return new ProductoRequest(
                "RC-30", "RC-30", "Mochila rolltop",
                new BigDecimal("45000"), new BigDecimal("39990"),
                10, 3, "/img/rc30.png", 1L);
    }


    @Test
    void crear_productoValido_loGuardaActivoYConCategoria() {

        Categoria mochilas = new Categoria(1L, "Mochilas", null);

        when(productoRepository.existsByCodigoIgnoreCase("RC-30")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(mochilas));
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Producto resultado = productoService.crear(requestValido());

        assertTrue(resultado.getActivo());
        assertEquals("RC-30", resultado.getCodigo());
        assertEquals("Mochilas", resultado.getCategoria().getNombre());
    }


    @Test
    void crear_codigoRepetido_lanzaErrorYNoGuarda() {

        when(productoRepository.existsByCodigoIgnoreCase("RC-30")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productoService.crear(requestValido()));

        verify(productoRepository, never()).save(any());
    }


    @Test
    void crear_codigoMuyCorto_lanzaError() {

        ProductoRequest datos = new ProductoRequest(
                "RC", "RC-30", null, new BigDecimal("45000"), null, 10, 3, null, 1L);

        assertThrows(IllegalArgumentException.class, () -> productoService.crear(datos));
    }


    @Test
    void crear_ofertaMayorAlPrecio_lanzaError() {

        ProductoRequest datos = new ProductoRequest(
                "RC-30", "RC-30", null,
                new BigDecimal("45000"), new BigDecimal("50000"),
                10, 3, null, 1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> productoService.crear(datos));

        assertTrue(error.getMessage().contains("oferta"));
    }


    @Test
    void crear_stockNegativo_lanzaError() {

        ProductoRequest datos = new ProductoRequest(
                "RC-30", "RC-30", null, new BigDecimal("45000"), null, -1, 3, null, 1L);

        assertThrows(IllegalArgumentException.class, () -> productoService.crear(datos));
    }


    @Test
    void crear_categoriaInexistente_lanzaError() {

        when(productoRepository.existsByCodigoIgnoreCase("RC-30")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> productoService.crear(requestValido()));
    }


    @Test
    void eliminar_noBorraSinoQueDesactiva() {

        Producto producto = new Producto();
        producto.setIdProducto(1L);
        producto.setActivo(true);

        when(productoRepository.buscarParaActualizar(1L)).thenReturn(Optional.of(producto));

        productoService.eliminar(1L);

        assertFalse(producto.getActivo());
        verify(productoRepository).save(producto);
        verify(productoRepository, never()).delete(any());
    }
}