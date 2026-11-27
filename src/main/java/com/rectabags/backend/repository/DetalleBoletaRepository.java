package com.rectabags.backend.repository;

import com.rectabags.backend.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {
    List<DetalleBoleta> findByBoletaIdBoletaOrderByIdDetalleBoletaAsc(Long idBoleta);
}
