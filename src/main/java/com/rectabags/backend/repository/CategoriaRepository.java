package com.rectabags.backend.repository;

import com.rectabags.backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // ¿Ya existe una categoría con ese nombre? (para no repetir)
    boolean existsByNombreIgnoreCase(String nombre);

    // Lo mismo, pero ignorando la que estoy editando
    boolean existsByNombreIgnoreCaseAndIdCategoriaNot(String nombre, Long idCategoria);
}