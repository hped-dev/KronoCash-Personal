package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.dto.PreferencesRequest;
import com.GIU26B.KronoCash.model.ThemeStyle;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
 * Preferencias de la primera visita (usuario anónimo). Se guardan en la sesión
 * HTTP para que el servidor pueda renderizar el tema correcto sin parpadeo;
 * cuando el usuario se registra/inicia sesión, accessibility.js las sincroniza
 * con user_preferences a través de /api/preferences.
 */
@Service
public class GuestPreferencesService {

    private static final String THEME = "kronocash.guest.theme";
    private static final String DARK_MODE = "kronocash.guest.darkMode";
    private static final String REDUCE_MOTION = "kronocash.guest.reduceMotion";
    private static final String FONT_SCALE = "kronocash.guest.fontScale";

    public PreferencesRequest read(HttpSession session) {
        PreferencesRequest preferences = new PreferencesRequest();
        preferences.setThemeStyle(readTheme(session));
        preferences.setDarkMode(Boolean.TRUE.equals(session.getAttribute(DARK_MODE)));
        preferences.setReduceMotion(Boolean.TRUE.equals(session.getAttribute(REDUCE_MOTION)));
        Object scale = session.getAttribute(FONT_SCALE);
        preferences.setFontScale(scale instanceof Integer value && value >= 100 && value <= 200 ? value : 100);
        return preferences;
    }

    public void save(HttpSession session, PreferencesRequest request) {
        session.setAttribute(THEME, ThemeStyle.fromValue(request.getThemeStyle()).getValue());
        session.setAttribute(DARK_MODE, request.isDarkMode());
        session.setAttribute(REDUCE_MOTION, request.isReduceMotion());
        session.setAttribute(FONT_SCALE, request.getFontScale());
    }

    private String readTheme(HttpSession session) {
        Object theme = session.getAttribute(THEME);
        if (theme instanceof String value) {
            try {
                return ThemeStyle.fromValue(value).getValue();
            } catch (IllegalArgumentException ignored) {
                // Valor de sesión corrupto: se descarta y se usa el default.
            }
        }
        return ThemeStyle.MINIMAL.getValue();
    }
}
