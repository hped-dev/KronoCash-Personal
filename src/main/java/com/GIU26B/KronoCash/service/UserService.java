package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.dto.PreferencesRequest;
import com.GIU26B.KronoCash.dto.RegisterRequest;
import com.GIU26B.KronoCash.model.ThemeStyle;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.model.UserPreferences;
import com.GIU26B.KronoCash.repository.UserPreferencesRepository;
import com.GIU26B.KronoCash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Registra al usuario con su contraseña cifrada con BCrypt y crea en la
     * misma transacción su fila de preferencias por defecto (tema 'minimal',
     * modo oscuro desactivado, sin reducción de movimiento, escala 100%).
     */
    public User register(RegisterRequest request) {
        if (existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Este nombre de usuario ya está registrado.");
        }
        if (existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Este correo electrónico ya está registrado.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user.setPreferences(UserPreferences.defaultFor(user));

        return userRepository.save(user);
    }

    /** Devuelve las preferencias existentes o las crea con valores por defecto. */
    public UserPreferences getOrCreatePreferences(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> preferencesRepository.save(createDefaultPreferences(userId)));
    }

    /** Actualiza tema y accesibilidad respetando los CHECK de la migración V1. */
    public UserPreferences updatePreferences(Long userId, PreferencesRequest request) {
        UserPreferences preferences = getOrCreatePreferences(userId);
        preferences.setThemeStyle(ThemeStyle.fromValue(request.getThemeStyle()));
        preferences.setDarkMode(request.isDarkMode());
        preferences.setReduceMotion(request.isReduceMotion());
        preferences.setFontScale(request.getFontScale());
        return preferencesRepository.save(preferences);
    }

    /** Instantánea para renderizar preferencias sin crear filas en una lectura. */
    @Transactional(readOnly = true)
    public PreferencesRequest getPreferencesSnapshot(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .map(UserService::toRequest)
                .orElseGet(() -> PreferencesRequest.builder().build());
    }

    public static PreferencesRequest toRequest(UserPreferences preferences) {
        return PreferencesRequest.builder()
                .themeStyle(preferences.getThemeStyle().getValue())
                .darkMode(preferences.isDarkMode())
                .reduceMotion(preferences.isReduceMotion())
                .fontScale(preferences.getFontScale())
                .build();
    }

    private UserPreferences createDefaultPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        return UserPreferences.defaultFor(user);
    }
}
