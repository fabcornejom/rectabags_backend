package com.rectabags.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.service.CategoriaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Mantenedor de categorías de productos")
public class CategoriaController {

    private final CategoriaService categoriaService;


    @GetMapping
    @Operation(summary = "Listar categorías")
    public List<Categoria> listar() {
        return categoriaService.listar();
    }


    @GetMapping("/{id}")
    @Operation(summary = "Ver una categoría")
    public Categoria buscar(@PathVariable Long id) {
        return categoriaService.buscarPorId(id);
    }


    @PostMapping
    @Operation(summary = "Crear categoría (ADMIN)")
    public ResponseEntity<Categoria> crear(@RequestBody Categoria categoria) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoriaService.crear(categoria));
    }


    @PutMapping("/{id}")
    @Operation(summary = "Editar categoría (ADMIN)")
    public Categoria actualizar(@PathVariable Long id, @RequestBody Categoria categoria) {
        return categoriaService.actualizar(id, categoria);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría (ADMIN)")
    public Map<String, String> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return Map.of("mensaje", "Categoría eliminada.");
    }


    // Errores de validación → 400 con el mensaje
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }


    // No encontrado → 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
    }
}