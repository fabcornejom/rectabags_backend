package com.rectabags.backend.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.rectabags.backend.repository.UsuarioRepository;
import com.rectabags.backend.security.JwtFilter;
import com.rectabags.backend.security.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.DispatcherType;

/*
 * Seguridad DEFINITIVA (reemplaza la temporal del Paso 0).
 * Las reglas se evalúan de arriba hacia abajo: la primera que coincide gana,
 * por eso las rutas específicas van antes que las generales.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtUtil jwtUtil,
            UsuarioRepository usuarioRepository) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            /*
             * Dos respuestas distintas:
             *  - 401: no hay sesión o el token no sirve.
             *  - 403: hay sesión, pero el rol no alcanza.
             */
            .exceptionHandling(e -> e
                .authenticationEntryPoint((peticion, respuesta, error) ->
                    responder(respuesta, HttpStatus.UNAUTHORIZED,
                        "Debes iniciar sesión para acceder a este recurso."))
                .accessDeniedHandler((peticion, respuesta, error) ->
                    responder(respuesta, HttpStatus.FORBIDDEN,
                        "No tienes permiso para realizar esta acción.")))

            .authorizeHttpRequests(auth -> auth

                // Un error interno conserva su estado HTTP; no es un nuevo login.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                // Swagger
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // Login, registro y regiones: todos
                .requestMatchers(HttpMethod.POST, API + "/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, API + "/regiones").permitAll()

                // Catálogo: ver es público, modificar es del ADMIN
                .requestMatchers(HttpMethod.GET, API + "/productos/**", API + "/categorias/**").permitAll()
                .requestMatchers(API + "/productos/**", API + "/categorias/**").hasRole("ADMIN")

                // Boletas
                .requestMatchers(HttpMethod.POST, API + "/boletas").permitAll()
                .requestMatchers(HttpMethod.GET, API + "/boletas/mias").authenticated()
                .requestMatchers(HttpMethod.GET, API + "/boletas").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.GET, API + "/boletas/{id}").authenticated() // el dueño se revisa en la Parte 3
                .requestMatchers(API + "/boletas/**").hasRole("ADMIN")

                // Reportes: solo ADMIN
                .requestMatchers(API + "/reportes/**").hasRole("ADMIN")

                // Usuarios: mi perfil para cualquier logueado, el resto solo ADMIN
                .requestMatchers(API + "/usuarios/me").authenticated()
                .requestMatchers(API + "/usuarios/**").hasRole("ADMIN")

                // Cualquier otra ruta exige estar logueado
                .anyRequest().authenticated())

            .addFilterBefore(
                    new JwtFilter(jwtUtil, usuarioRepository),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Escribe la respuesta de error en el mismo formato JSON que usa el resto del backend
    private static void responder(
            HttpServletResponse respuesta,
            HttpStatus estado,
            String mensaje) throws IOException {

        respuesta.setStatus(estado.value());
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }

    // Permite que el frontend (React en el puerto 5173) llame al backend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
