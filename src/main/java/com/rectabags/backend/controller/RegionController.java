package com.rectabags.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rectabags.backend.dto.RegionDTO;
import com.rectabags.backend.service.RegionService;

@RestController
@RequestMapping("/api/v1/regiones")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public List<RegionDTO> listar() {
        return regionService.listar();
    }
}
