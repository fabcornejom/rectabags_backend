package com.rectabags.backend.dto;

import com.rectabags.backend.model.EstadoBoleta;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(
        @NotNull(message = "El estado es obligatorio.") EstadoBoleta estado) {
}
