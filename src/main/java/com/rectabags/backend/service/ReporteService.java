package com.rectabags.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rectabags.backend.dto.BoletaDTO;
import com.rectabags.backend.dto.ResumenDTO;
import com.rectabags.backend.dto.ResumenDTO.ProductoVendido;
import com.rectabags.backend.model.EstadoBoleta;
import com.rectabags.backend.model.Boleta;
import com.rectabags.backend.repository.BoletaRepository;
import java.time.LocalDateTime;

@Service
public class ReporteService {
    private final BoletaService boletas;
    private final BoletaRepository repositorio;

    public ReporteService(BoletaService boletas, BoletaRepository repositorio) {
        this.boletas = boletas;
        this.repositorio = repositorio;
    }

    @Transactional
    public long cerrarPeriodo() {
        List<Boleta> abiertas = repositorio.abiertasParaCerrar();
        LocalDateTime cierre = LocalDateTime.now();
        abiertas.forEach(boleta -> boleta.setFechaCierre(cierre));
        repositorio.saveAll(abiertas);
        return abiertas.size();
    }

    @Transactional(readOnly = true)
    public ResumenDTO resumen() {
        List<BoletaDTO> abiertas = boletas.listar().stream()
                .filter(boleta -> boleta.fechaCierre() == null).toList();
        Map<String, Long> porEstado = new LinkedHashMap<>();
        Map<Long, ProductoVendido> porProducto = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        long ordenes = 0;
        long unidades = 0;
        for (BoletaDTO boleta : abiertas) {
            porEstado.merge(boleta.estado().name(), 1L, Long::sum);
            if (boleta.estado() == EstadoBoleta.CANCELADA) continue;
            ordenes++;
            total = total.add(boleta.total());
            for (BoletaDTO.DetalleDTO detalle : boleta.detalles()) {
                unidades += detalle.cantidad();
                ProductoVendido anterior = porProducto.get(detalle.idProducto());
                porProducto.put(detalle.idProducto(), new ProductoVendido(detalle.idProducto(),
                        anterior == null ? detalle.nombreProducto() : anterior.nombre(),
                        (anterior == null ? 0 : anterior.unidades()) + detalle.cantidad(),
                        (anterior == null ? BigDecimal.ZERO : anterior.total()).add(detalle.subTotal())));
            }
        }
        List<ProductoVendido> ranking = new ArrayList<>(porProducto.values());
        ranking.sort(Comparator.comparingLong(ProductoVendido::unidades).reversed());
        // El reporte anterior redondea el ticket al peso chileno más cercano.
        BigDecimal ticket = ordenes == 0 ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(ordenes), 0, RoundingMode.HALF_UP);
        return new ResumenDTO(ordenes, total, unidades, ticket,
                ranking.isEmpty() ? null : ranking.get(0), ranking, porEstado);
    }
}
