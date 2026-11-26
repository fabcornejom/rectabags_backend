package com.rectabags.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemCompraDTO(
        @NotNull(message = "El producto es obligatorio.")
        @Min(value = 1, message = "El identificador del producto debe ser positivo.")
        Long idProducto,
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        Integer cantidad) {
}
