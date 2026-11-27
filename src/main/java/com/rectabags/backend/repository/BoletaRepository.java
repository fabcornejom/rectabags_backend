package com.rectabags.backend.repository;

import com.rectabags.backend.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Boleta b where b.idBoleta = :id")
    Optional<Boleta> buscarParaActualizar(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Boleta b where b.fechaCierre is null order by b.idBoleta")
    List<Boleta> abiertasParaCerrar();
    List<Boleta> findAllByOrderByFechaEmisionDescIdBoletaDesc();
    List<Boleta> findByUsuarioIdUsuarioOrderByFechaEmisionDescIdBoletaDesc(Long idUsuario);
}
