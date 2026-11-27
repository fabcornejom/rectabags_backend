package com.rectabags.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    // Repositorios falsos (mocks)
    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    // Service real, que recibe los mocks
    @InjectMocks
    private CategoriaService categoriaService;


    @Test
    void crear_categoriaValida_laGuardaSinEspacios() {

        when(categoriaRepository.existsByNombreIgnoreCase("Bolsos")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Categoria resultado = categoriaService.crear(new Categoria(null, "  Bolsos  ", "Bolsos de bici"));

        assertEquals("Bolsos", resultado.getNombre());
        verify(categoriaRepository).save(any(Categoria.class));
    }


    @Test
    void crear_nombreRepetido_lanzaErrorYNoGuarda() {

        when(categoriaRepository.existsByNombreIgnoreCase("Mochilas")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> categoriaService.crear(new Categoria(null, "Mochilas", null)));

        verify(categoriaRepository, never()).save(any());
    }


    @Test
    void crear_nombreVacio_lanzaError() {

        assertThrows(IllegalArgumentException.class,
                () -> categoriaService.crear(new Categoria(null, "   ", null)));
    }


    @Test
    void buscarPorId_noExiste_lanzaNoEncontrado() {

        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> categoriaService.buscarPorId(99L));
    }


    @Test
    void eliminar_conProductosAsociados_lanzaErrorYNoElimina() {

        Categoria categoria = new Categoria(1L, "Mochilas", null);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.existsByCategoriaIdCategoria(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoriaService.eliminar(1L));

        verify(categoriaRepository, never()).delete(any());
    }


    @Test
    void eliminar_sinProductos_laElimina() {

        Categoria categoria = new Categoria(1L, "Mochilas", null);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.existsByCategoriaIdCategoria(1L)).thenReturn(false);

        categoriaService.eliminar(1L);

        verify(categoriaRepository).delete(categoria);
    }
}
