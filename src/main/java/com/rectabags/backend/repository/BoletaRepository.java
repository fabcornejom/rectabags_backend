package com.rectabags.backend.repository;

import com.rectabags.backend.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    List<Boleta> findAllByOrderByFechaEmisionDescIdBoletaDesc();
    List<Boleta> findByUsuarioIdUsuarioOrderByFechaEmisionDescIdBoletaDesc(Long idUsuario);
}
