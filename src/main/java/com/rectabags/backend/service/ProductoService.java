package com.rectabags.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.rectabags.backend.dto.ProductoRequest;
import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;


    // Lista los activos; si llega una categoría, filtra por ella
    public List<Producto> listar(Long idCategoria) {

        if (idCategoria != null) {
            return productoRepository
                    .findByActivoTrueAndCategoriaIdCategoriaOrderByNombreAsc(idCategoria);
        }

        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }


    public List<Producto> listarOfertas() {
        return productoRepository.findByActivoTrueAndPrecioOfertaIsNotNullOrderByNombreAsc();
    }


    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado."));
    }


    public Producto crear(ProductoRequest datos) {

        validar(datos);

        if (productoRepository.existsByCodigoIgnoreCase(datos.codigo().trim())) {
            throw new IllegalArgumentException("Ya existe un producto con ese código.");
        }

        Producto producto = new Producto();
        copiarDatos(datos, producto);
        producto.setActivo(true);

        return productoRepository.save(producto);
    }


    public Producto actualizar(Long id, ProductoRequest datos) {

        Producto producto = buscarPorId(id);

        validar(datos);

        if (productoRepository.existsByCodigoIgnoreCaseAndIdProductoNot(datos.codigo().trim(), id)) {
            throw new IllegalArgumentException("Ya existe un producto con ese código.");
        }

        copiarDatos(datos, producto);

        return productoRepository.save(producto);
    }


    // Borrado lógico: no se borra de la tabla, solo se desactiva.
    // Así las boletas antiguas siguen funcionando.
    public void eliminar(Long id) {

        Producto producto = buscarPorId(id);

        producto.setActivo(false);
        productoRepository.save(producto);
    }


    // Pasa los datos del DTO a la entidad
    private void copiarDatos(ProductoRequest datos, Producto producto) {

        Categoria categoria = categoriaRepository.findById(datos.idCategoria())
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));

        producto.setCodigo(datos.codigo().trim());
        producto.setNombre(datos.nombre().trim());
        producto.setDescripcion(datos.descripcion());
        producto.setPrecio(datos.precio());
        producto.setPrecioOferta(datos.precioOferta());
        producto.setStock(datos.stock());
        producto.setStockCritico(datos.stockCritico() != null ? datos.stockCritico() : 0);
        producto.setImagen(datos.imagen());
        producto.setCategoria(categoria);
    }


    // Reglas de negocio del producto
    private void validar(ProductoRequest datos) {

        // Código: requerido, de 3 a 20
        if (datos.codigo() == null || datos.codigo().trim().length() < 3) {
            throw new IllegalArgumentException("El código debe tener al menos 3 caracteres.");
        }
        if (datos.codigo().trim().length() > 20) {
            throw new IllegalArgumentException("El código no puede superar 20 caracteres.");
        }

        // Nombre: requerido, máx. 100
        if (datos.nombre() == null || datos.nombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (datos.nombre().trim().length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres.");
        }

        // Descripción: opcional, máx. 200
        if (datos.descripcion() != null && datos.descripcion().length() > 200) {
            throw new IllegalArgumentException("La descripción no puede superar 200 caracteres.");
        }

        // Precio: requerido, mínimo 0 (puede tener decimales)
        if (datos.precio() == null || datos.precio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio debe ser 0 o mayor.");
        }

        // Precio de oferta: opcional, mayor que 0 y menor que el precio
        if (datos.precioOferta() != null) {
            if (datos.precioOferta().compareTo(BigDecimal.ZERO) <= 0
                    || datos.precioOferta().compareTo(datos.precio()) >= 0) {
                throw new IllegalArgumentException(
                        "El precio de oferta debe ser mayor que 0 y menor que el precio.");
            }
        }

        // Stock: requerido, entero, mínimo 0
        if (datos.stock() == null || datos.stock() < 0) {
            throw new IllegalArgumentException("El stock debe ser 0 o mayor.");
        }

        // Stock crítico: opcional, mínimo 0
        if (datos.stockCritico() != null && datos.stockCritico() < 0) {
            throw new IllegalArgumentException("El stock crítico debe ser 0 o mayor.");
        }

        // Imagen: opcional, máx. 100
        if (datos.imagen() != null && datos.imagen().length() > 100) {
            throw new IllegalArgumentException("La ruta de la imagen no puede superar 100 caracteres.");
        }

        // Categoría: requerida
        if (datos.idCategoria() == null) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }
    }
}