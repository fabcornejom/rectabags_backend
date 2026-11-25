package com.rectabags.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.rectabags.backend.dto.ProductoRequest;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo y mantenedor de productos")
public class ProductoController {

    private final ProductoService productoService;


    // GET /productos  o  GET /productos?categoria=1
    @GetMapping
    @Operation(summary = "Listar productos activos (opcional: filtrar por categoría)")
    public List<Producto> listar(@RequestParam(name = "categoria", required = false) Long idCategoria) {
        return productoService.listar(idCategoria);
    }


    @GetMapping("/ofertas")
    @Operation(summary = "Listar productos con oferta")
    public List<Producto> ofertas() {
        return productoService.listarOfertas();
    }


    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto")
    public Producto buscar(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }


    @PostMapping
    @Operation(summary = "Crear producto (ADMIN)")
    public ResponseEntity<Producto> crear(@RequestBody ProductoRequest datos) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productoService.crear(datos));
    }


    @PutMapping("/{id}")
    @Operation(summary = "Editar producto (ADMIN)")
    public Producto actualizar(@PathVariable Long id, @RequestBody ProductoRequest datos) {
        return productoService.actualizar(id, datos);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto (ADMIN) – borrado lógico")
    public Map<String, String> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return Map.of("mensaje", "Producto eliminado.");
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }


    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
    }
}