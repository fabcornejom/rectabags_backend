package com.rectabags.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompraRequest(
        @NotBlank(message = "El nombre es obligatorio.") @Size(max = 50)
        String nombreCliente,
        @NotBlank(message = "El apellido es obligatorio.") @Size(max = 50)
        String apellidoCliente,
        @NotBlank(message = "El correo es obligatorio.") @Email @Size(max = 100)
        @Pattern(regexp = "(?i)^[^\\s@]+@(gmail\\.com|duoc\\.cl|profesor\\.duoc\\.cl)$",
                message = "Solo se permiten correos @gmail.com, @duoc.cl o @profesor.duoc.cl.")
        String correoCliente,
        @NotBlank(message = "El teléfono es obligatorio.") @Size(max = 20)
        @Pattern(regexp = "^(?:\\+?56\\s*)?9(?:\\s*\\d){8}$",
                message = "Ingresa un celular chileno válido de 9 dígitos.")
        String telefonoCliente,
        @NotBlank(message = "La dirección es obligatoria.") @Size(max = 120)
        String direccionCliente,
        @NotBlank(message = "La región es obligatoria.") @Size(max = 30)
        String region,
        @NotBlank(message = "La comuna es obligatoria.") @Size(max = 30)
        String comuna,
        @NotBlank(message = "El método de pago es obligatorio.") @Size(max = 30)
        String metodoPago,
        @NotEmpty(message = "La compra debe contener al menos un producto.")
        List<@NotNull @Valid ItemCompraDTO> items) {
}
