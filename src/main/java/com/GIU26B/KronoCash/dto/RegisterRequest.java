package com.GIU26B.KronoCash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Formulario de registro. No incluye "confirmar contraseña" ni desafíos
 * cognitivos: WCAG 2.2 SC 3.3.8 permite explícitamente gestores de contraseñas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio.")
    @Size(min = 3, max = 50, message = "Debe tener entre 3 y 50 caracteres.")
    private String username;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Introduzca un correo electrónico válido.")
    @Size(max = 100, message = "No puede superar los 100 caracteres.")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 128, message = "Debe tener entre 8 y 128 caracteres.")
    private String password;

    /* Se recortan espacios para que coincida con la comprobación de duplicados. */
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
