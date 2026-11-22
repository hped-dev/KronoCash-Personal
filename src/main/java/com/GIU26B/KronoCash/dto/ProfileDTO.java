package com.GIU26B.KronoCash.dto;

import com.GIU26B.KronoCash.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Datos públicos del perfil: nunca expone el hash de la contraseña. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {

    private String username;
    private String email;
    private LocalDateTime createdAt;

    public static ProfileDTO from(User user) {
        return ProfileDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
