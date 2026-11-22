package com.GIU26B.KronoCash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configuración de rutas públicas y protegidas
            .authorizeHttpRequests(auth -> auth
                // Permitir acceso libre a recursos estáticos (CSS, JS, imágenes) y páginas públicas
                .requestMatchers("/", "/css/**", "/js/**", "/img/**", "/register", "/login", "/error",
                        "/api/preferences").permitAll()
                // Cualquier otra vista (dashboard, simulador, ajustes) requiere autenticación
                .anyRequest().authenticated()
            )
            // Configuración del formulario de inicio de sesión
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            // Configuración de cierre de sesión
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // CSRF activo también en /api/**: los scripts envían el token leído de <meta>.
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(content -> { })
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
                    "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                    + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://fonts.cdnfonts.com; "
                    + "font-src 'self' data: https://fonts.gstatic.com https://fonts.cdnfonts.com; "
                    + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'"))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Encriptador robusto de contraseñas compatible con Spring Security 6 / Java 25
        return new BCryptPasswordEncoder();
    }
}