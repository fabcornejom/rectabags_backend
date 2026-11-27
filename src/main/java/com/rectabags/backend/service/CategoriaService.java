package com.rectabags.backend.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;


    public List<Categoria> listar() {
        return categoriaRepository.findAll(Sort.by("nombre"));
    }


    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Categoría no encontrada."));
    }


    public Categoria crear(Categoria datos) {

        validar(datos);

        if (categoriaRepository.existsByNombreIgnoreCase(datos.getNombre())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }

        datos.setIdCategoria(null); // el id lo pone la base de datos
        return categoriaRepository.save(datos);
    }


    public Categoria actualizar(Long id, Categoria datos) {

        Categoria categoria = buscarPorId(id);

        validar(datos);

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdCategoriaNot(datos.getNombre(), id)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }

        categoria.setNombre(datos.getNombre());
        categoria.setDescripcion(datos.getDescripcion());

        return categoriaRepository.save(categoria);
    }


    public void eliminar(Long id) {

        Categoria categoria = buscarPorId(id);

        if (productoRepository.existsByCategoriaIdCategoria(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: la categoría tiene productos asociados, incluidos los inactivos.");
        }

        categoriaRepository.delete(categoria);
    }


    // Reglas: nombre requerido (máx. 50) y descripción opcional (máx. 200)
    private void validar(Categoria datos) {

        if (datos.getNombre() == null || datos.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }

        datos.setNombre(datos.getNombre().trim());

        if (datos.getNombre().length() > 50) {
            throw new IllegalArgumentException("El nombre no puede superar 50 caracteres.");
        }

        if (datos.getDescripcion() != null && datos.getDescripcion().length() > 200) {
            throw new IllegalArgumentException("La descripción no puede superar 200 caracteres.");
        }
    }
}
