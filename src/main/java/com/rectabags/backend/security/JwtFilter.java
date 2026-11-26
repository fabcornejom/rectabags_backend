package com.rectabags.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.rectabags.backend.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * Se ejecuta en CADA petición. Si trae "Authorization: Bearer <token>" y el
 * token es válido, deja al usuario como autenticado con su rol.
 * Si no trae token (o es inválido), la petición sigue sin autenticar y
 * SecurityConfig decide si la ruta lo permite o responde 401.
 *
 * El usuario se vuelve a leer de la base de datos: así, si el admin cambia
 * un rol o elimina a alguien, el cambio vale de inmediato.
 */
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public JwtFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith("Bearer ")) {

            String token = cabecera.substring(7);

            jwtUtil.idDesdeToken(token)
                    .flatMap(usuarioRepository::findById)
                    .ifPresent(usuario -> {

                        var autenticacion = new UsernamePasswordAuthenticationToken(
                                String.valueOf(usuario.getIdUsuario()),   // getName() = id
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));

                        // Se crea un contexto nuevo y se instala completo
                        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                        contexto.setAuthentication(autenticacion);
                        SecurityContextHolder.setContext(contexto);
                    });
        }

        chain.doFilter(request, response);
    }
}