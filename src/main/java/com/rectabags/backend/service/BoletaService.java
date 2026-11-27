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
import org.springframework.security.access.AccessDeniedException;

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
import com.rectabags.backend.repository.UsuarioRepository;
import com.rectabags.backend.exception.RecursoNoEncontradoException;

@Service
public class BoletaService {

    private final BoletaRepository boletaRepository;
    private final DetalleBoletaRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public BoletaService(BoletaRepository boletaRepository,
            DetalleBoletaRepository detalleRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository) {
        this.boletaRepository = boletaRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public BoletaDTO crearCompra(CompraRequest request) {
        return crearCompra(request, null);
    }

    @Transactional
    public BoletaDTO crearCompra(CompraRequest request, Long idUsuario) {
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
        if (idUsuario != null) {
            boleta.setUsuario(usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado.")));
        }
        boletaRepository.save(boleta);

        for (DetalleBoleta detalle : detalles) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() - detalle.getCantidad());
            detalle.setBoleta(boleta);
        }
        productoRepository.saveAll(productos);
        List<DetalleBoleta> guardados = detalleRepository.saveAll(detalles);

        return aDTO(boleta, guardados);
    }

    @Transactional(readOnly = true)
    public List<BoletaDTO> listar() {
        return boletaRepository.findAllByOrderByFechaEmisionDescIdBoletaDesc().stream()
                .map(this::conDetalles).toList();
    }

    @Transactional(readOnly = true)
    public List<BoletaDTO> historial(Long idUsuario) {
        return boletaRepository.findByUsuarioIdUsuarioOrderByFechaEmisionDescIdBoletaDesc(idUsuario)
                .stream().map(this::conDetalles).toList();
    }

    @Transactional(readOnly = true)
    public BoletaDTO obtener(Long id, Long idUsuario, boolean accesoPersonal) {
        Boleta boleta = boletaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta no encontrada."));
        boolean esDueno = boleta.getUsuario() != null
                && boleta.getUsuario().getIdUsuario().equals(idUsuario);
        if (!accesoPersonal && !esDueno) {
            throw new AccessDeniedException("No tienes permiso para consultar esta boleta.");
        }
        return conDetalles(boleta);
    }

    private BoletaDTO conDetalles(Boleta boleta) {
        return aDTO(boleta, detalleRepository.findByBoletaIdBoletaOrderByIdDetalleBoletaAsc(boleta.getIdBoleta()));
    }

    private BoletaDTO aDTO(Boleta boleta, List<DetalleBoleta> guardados) {
        return new BoletaDTO(boleta.getIdBoleta(), boleta.getFolio(),
                boleta.getUsuario() == null ? null : boleta.getUsuario().getIdUsuario(),
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
