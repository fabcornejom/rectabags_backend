package com.rectabags.backend.repository;

import java.util.List;

import com.rectabags.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // ¿La categoría tiene productos activos? (para no eliminarla)
    boolean existsByCategoriaIdCategoriaAndActivoTrue(Long idCategoria);

    // Todos los productos activos, ordenados por nombre
    List<Producto> findByActivoTrueOrderByNombreAsc();

    // Productos activos de una categoría
    List<Producto> findByActivoTrueAndCategoriaIdCategoriaOrderByNombreAsc(Long idCategoria);

    // Productos activos con oferta
    List<Producto> findByActivoTrueAndPrecioOfertaIsNotNullOrderByNombreAsc();

    // ¿Ya existe ese código? (al crear y al editar)
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdProductoNot(String codigo, Long idProducto);
}