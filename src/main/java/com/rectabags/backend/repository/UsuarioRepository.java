package com.rectabags.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rectabags.backend.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRun(String run);

    // Al editar: el correo o el RUN pueden "repetirse" solo con el mismo usuario
    boolean existsByCorreoAndIdUsuarioNot(String correo, Long idUsuario);

    boolean existsByRunAndIdUsuarioNot(String run, Long idUsuario);
}
