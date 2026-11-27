package com.rectabags.backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.rectabags.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.idProducto = :id")
    Optional<Producto> buscarParaActualizar(@Param("id") Long id);

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
