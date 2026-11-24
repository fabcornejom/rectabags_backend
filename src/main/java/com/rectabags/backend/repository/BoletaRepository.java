package com.rectabags.backend.repository;

import com.rectabags.backend.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
}