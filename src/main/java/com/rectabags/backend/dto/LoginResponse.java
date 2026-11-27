package com.rectabags.backend.dto;

public record LoginResponse(String token, UsuarioDTO usuario) {
}
