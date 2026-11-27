package com.rectabags.backend.repository;

import com.rectabags.backend.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {
    @Query("select d.producto.idProducto from DetalleBoleta d where d.boleta.idBoleta = :id order by d.producto.idProducto")
    List<Long> productosDeBoleta(@Param("id") Long id);
    List<DetalleBoleta> findByBoletaIdBoletaOrderByIdDetalleBoletaAsc(Long idBoleta);
}
