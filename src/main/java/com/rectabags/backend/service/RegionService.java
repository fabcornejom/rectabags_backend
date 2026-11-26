package com.rectabags.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rectabags.backend.dto.RegionDTO;

/*
 * Regiones y comunas disponibles. Son las mismas que usa el frontend
 * (src/data/regionesComunas.js). No hay tabla en la base de datos:
 * región y comuna se guardan como texto en USUARIO.
 */
@Service
public class RegionService {

    private static final List<RegionDTO> REGIONES = List.of(
        new RegionDTO("Región Metropolitana", List.of("Santiago", "Providencia", "Ñuñoa", "Las Condes", "La Florida", "Maipú", "Til Til", "Puente Alto")),
        new RegionDTO("Región de Valparaíso", List.of("Valparaíso", "Viña del Mar", "Concón", "Quilpué", "Villa Alemana", "Quillota", "San Antonio")),
        new RegionDTO("Región del Biobío", List.of("Concepción", "Talcahuano", "San Pedro de la Paz", "Chiguayante", "Hualpén", "Coronel", "Los Ángeles")));

    public List<RegionDTO> listar() {
        return REGIONES;
    }
}
