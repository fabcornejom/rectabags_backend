package com.rectabags.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_boleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleBoleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_boleta")
    private Long idDetalleBoleta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_boleta", nullable = false)
    private Boleta boleta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "nombre_producto", nullable = false, length = 100)
    private String nombreProducto;

    @Column(name = "precio_unidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnidad;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "sub_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal subTotal;
}