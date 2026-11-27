package com.rectabags.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;

import com.rectabags.backend.dto.ResumenDTO;
import com.rectabags.backend.service.ReporteService;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {
    private final ReporteService reportes;

    public ReporteController(ReporteService reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/resumen")
    public ResumenDTO resumen() {
        return reportes.resumen();
    }

    @PostMapping("/cierre")
    public Map<String, Long> cerrarPeriodo() {
        return Map.of("cantidadCerradas", reportes.cerrarPeriodo());
    }
}
