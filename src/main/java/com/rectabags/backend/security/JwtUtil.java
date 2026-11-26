package com.rectabags.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rectabags.backend.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/*
 * Crea y valida los tokens JWT.
 * El token lleva: id del usuario (subject), correo, rol y fecha de expiración.
 */
@Component
public class JwtUtil {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secreto,
            @Value("${jwt.expiration-ms}") long expiracionMs) {

        // HS256 exige una clave de al menos 32 caracteres
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Usuario usuario) {

        Date ahora = new Date();

        return Jwts.builder()
                .subject(String.valueOf(usuario.getIdUsuario()))
                .claim("correo", usuario.getCorreo())
                .claim("rol", usuario.getRol().name())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /**
     * Si el token es válido (firma correcta y no vencido) devuelve el id del
     * usuario. Si no, devuelve vacío.
     */
    public Optional<Long> idDesdeToken(String token) {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(Long.valueOf(claims.getSubject()));

        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
