package com.rectabags.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rectabags.backend.dto.BoletaDTO;
import com.rectabags.backend.dto.CompraRequest;
import com.rectabags.backend.dto.ItemCompraDTO;
import com.rectabags.backend.model.Boleta;
import com.rectabags.backend.model.DetalleBoleta;
import com.rectabags.backend.model.EstadoBoleta;
import com.rectabags.backend.model.Producto;
import com.rectabags.backend.repository.BoletaRepository;
import com.rectabags.backend.repository.DetalleBoletaRepository;
import com.rectabags.backend.repository.ProductoRepository;

@Service
public class BoletaService {

    private final BoletaRepository boletaRepository;
    private final DetalleBoletaRepository detalleRepository;
    private final ProductoRepository productoRepository;

    public BoletaService(BoletaRepository boletaRepository,
            DetalleBoletaRepository detalleRepository, ProductoRepository productoRepository) {
        this.boletaRepository = boletaRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public BoletaDTO crearCompra(CompraRequest request) {
        Map<Long, Integer> cantidades = agruparCantidades(request.items());
        List<DetalleBoleta> detalles = new ArrayList<>();
        List<Producto> productos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Se comprueba toda la compra antes de modificar stock o guardar registros.
        for (Map.Entry<Long, Integer> item : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(item.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe el producto con identificador " + item.getKey() + "."));

            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " no está activo.");
            }
            if (producto.getStock() == null || producto.getStock() < item.getValue()) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre() + ".");
            }

            BigDecimal precio = producto.getPrecioOferta() != null
                    ? producto.getPrecioOferta() : producto.getPrecio();
            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setPrecioUnidad(precio);
            detalle.setCantidad(item.getValue());
            detalle.setSubTotal(precio.multiply(BigDecimal.valueOf(item.getValue())));
            total = total.add(detalle.getSubTotal());
            detalles.add(detalle);
            productos.add(producto);
        }

        Boleta boleta = crearBoletaInvitado(request, total);
        boletaRepository.save(boleta);

        for (DetalleBoleta detalle : detalles) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() - detalle.getCantidad());
            detalle.setBoleta(boleta);
        }
        productoRepository.saveAll(productos);
        List<DetalleBoleta> guardados = detalleRepository.saveAll(detalles);

        return new BoletaDTO(boleta.getIdBoleta(), boleta.getFolio(), null,
                boleta.getNombreCliente(), boleta.getApellidoCliente(), boleta.getCorreoCliente(),
                boleta.getTelefonoCliente(), boleta.getDireccionCliente(), boleta.getRegion(),
                boleta.getComuna(), boleta.getMetodoPago(), boleta.getSubtotal(), boleta.getTotal(),
                boleta.getEstado(), boleta.getFechaEmision(), boleta.getFechaCierre(),
                guardados.stream().map(detalle -> new BoletaDTO.DetalleDTO(
                        detalle.getIdDetalleBoleta(), detalle.getProducto().getIdProducto(),
                        detalle.getNombreProducto(), detalle.getPrecioUnidad(), detalle.getCantidad(),
                        detalle.getSubTotal())).toList());
    }

    private Map<Long, Integer> agruparCantidades(List<ItemCompraDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La compra debe contener al menos un producto.");
        }
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemCompraDTO item : items) {
            if (item == null || item.idProducto() == null || item.idProducto() < 1
                    || item.cantidad() == null || item.cantidad() < 1) {
                throw new IllegalArgumentException("Cada ítem debe tener un producto y una cantidad positiva.");
            }
            long cantidad = (long) cantidades.getOrDefault(item.idProducto(), 0) + item.cantidad();
            if (cantidad > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La cantidad del producto " + item.idProducto() + " es inválida.");
            }
            cantidades.put(item.idProducto(), (int) cantidad);
        }
        return cantidades;
    }

    private Boleta crearBoletaInvitado(CompraRequest request, BigDecimal total) {
        LocalDateTime ahora = LocalDateTime.now();
        Boleta boleta = new Boleta();
        boleta.setFolio("RB-" + ahora.getYear() + "-" + UUID.randomUUID().toString().replace("-", ""));
        boleta.setUsuario(null);
        boleta.setNombreCliente(request.nombreCliente().trim());
        boleta.setApellidoCliente(request.apellidoCliente().trim());
        boleta.setCorreoCliente(request.correoCliente().toLowerCase(java.util.Locale.ROOT));
        boleta.setTelefonoCliente(request.telefonoCliente().trim());
        boleta.setDireccionCliente(request.direccionCliente().trim());
        boleta.setRegion(request.region().trim());
        boleta.setComuna(request.comuna().trim());
        boleta.setMetodoPago(request.metodoPago().trim());
        boleta.setSubtotal(total);
        boleta.setTotal(total);
        boleta.setEstado(EstadoBoleta.PENDIENTE);
        boleta.setFechaEmision(ahora);
        return boleta;
    }
}
