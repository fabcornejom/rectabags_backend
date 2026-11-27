package com.rectabags.backend.controller;

import java.util.Map;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rectabags.backend.dto.BoletaDTO;
import com.rectabags.backend.dto.CompraRequest;
import com.rectabags.backend.dto.CambioEstadoRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import com.rectabags.backend.service.BoletaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/boletas")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    public ResponseEntity<BoletaDTO> crearCompra(@Valid @RequestBody CompraRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boletaService.crearCompra(request, idActual(authentication)));
    }

    @GetMapping
    public List<BoletaDTO> listar() {
        return boletaService.listar();
    }

    @PatchMapping("/{id}/estado")
    public BoletaDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return boletaService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/mias")
    public List<BoletaDTO> mias(Authentication authentication) {
        return boletaService.historial(idActual(authentication));
    }

    @GetMapping("/usuario/{id}")
    public List<BoletaDTO> historial(@PathVariable Long id) {
        return boletaService.historial(id);
    }

    @GetMapping("/{id}")
    public BoletaDTO obtener(@PathVariable Long id, Authentication authentication) {
        boolean personal = authentication.getAuthorities().stream()
                .anyMatch(rol -> rol.getAuthority().equals("ROLE_ADMIN") || rol.getAuthority().equals("ROLE_VENDEDOR"));
        return boletaService.obtener(id, idActual(authentication), personal);
    }

    private Long idActual(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken
                ? null : Long.valueOf(authentication.getName());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accesoDenegado(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> compraInvalida(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }
}
