package com.rectabags.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.rectabags.backend.model.Categoria;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.CategoriaRepository;
import com.rectabags.backend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

/*
 * Carga las categorías y los productos de RectaBags
 * la primera vez que arranca el backend (tablas vacías).
 */
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;


    @Override
    public void run(String... args) {

        // Si ya hay datos, no hace nada
        if (categoriaRepository.count() > 0 || productoRepository.count() > 0) {
            return;
        }

        Categoria mochilas = crearCategoria("Mochilas", "Mochilas rolltop para ciudad y viajes");
        Categoria cilindros = crearCategoria("Cilindros", "Bolsos de manillar cilíndricos");
        Categoria musette = crearCategoria("Musette", "Bolsos cruzados estilo musette");
        Categoria otros = crearCategoria("Otros", "Accesorios y productos especiales");

        crearProducto("RC-30", "RC-30", mochilas, 45000, null, 10,
                "Mochila rolltop diseñada para uso urbano y viajes.",
                "/img/Mochila RC-30/MOCHILA_MAIN.png");

        crearProducto("CIL-01", "Cilindro", cilindros, 32000, null, 10,
                "Bolso cilíndrico para equipamiento de bikepacking.",
                "/img/CILINDRO/CILINDRO_MAIN.png");

        crearProducto("MUS-01", "Musette", musette, 18000, 14990, 10,
                "Bolso ligero inspirado en las clásicas musette de ciclismo.",
                "/img/Musette/Musette_main.png");

        crearProducto("CIL-TH", "Cilindro x Thomson", cilindros, 35000, null, 10,
                "Cilindro colaboración con Thomson.",
                "/img/FOTOS_EJEMPLOS_TEMP/cilindro_th.png");

        crearProducto("CIL-VD", "Cilindro Verde", cilindros, 32000, null, 10,
                "Cilindro verde customizado.",
                "/img/FOTOS_EJEMPLOS_TEMP/cilindro_vd.png");

        crearProducto("HIP-01", "Hippouch porta U-Lock", otros, 40000, 34990, 10,
                "Hippouch porta U-Lock customizable de Rectabags.",
                "/img/FOTOS_EJEMPLOS_TEMP/hippopunch.png");

        crearProducto("EVA-01", "EVA-01", otros, 35000, null, 10,
                "EVA-01 customizado.",
                "/img/FOTOS_EJEMPLOS_TEMP/vea01.png");

        crearProducto("RC-30-VD", "RC-30 Verde", mochilas, 44000, null, 10,
                "RC-30 verde customizada.",
                "/img/FOTOS_EJEMPLOS_TEMP/mochi.png");

        crearProducto("RC-30-RS", "RC-30 Rosa", mochilas, 44000, null, 10,
                "RC-30 rosa customizada.",
                "/img/FOTOS_EJEMPLOS_TEMP/mochila_r.png");
    }


    private Categoria crearCategoria(String nombre, String descripcion) {
        return categoriaRepository.save(new Categoria(null, nombre, descripcion));
    }


    private void crearProducto(String codigo, String nombre, Categoria categoria,
                               int precio, Integer precioOferta, int stock,
                               String descripcion, String imagen) {

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setPrecio(BigDecimal.valueOf(precio));
        producto.setPrecioOferta(precioOferta != null ? BigDecimal.valueOf(precioOferta) : null);
        producto.setStock(stock);
        producto.setStockCritico(3);
        producto.setDescripcion(descripcion);
        producto.setImagen(imagen);
        producto.setActivo(true);

        productoRepository.save(producto);
    }
}