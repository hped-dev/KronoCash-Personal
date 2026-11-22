package com.GIU26B.KronoCash.controller;

import com.GIU26B.KronoCash.dto.PreferencesRequest;
import com.GIU26B.KronoCash.repository.UserRepository;
import com.GIU26B.KronoCash.service.AuthenticationHelper;
import com.GIU26B.KronoCash.service.GuestPreferencesService;
import com.GIU26B.KronoCash.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Expone el estado de accesibilidad en TODAS las vistas de identidad, de modo
 * que <html data-theme=...> se renderiza en servidor (sin parpadeo) y el JS
 * solo necesita reaccionar a los cambios del usuario.
 */
@ControllerAdvice(assignableTypes = {HomeController.class, AuthController.class,
        SettingsController.class, DashboardController.class, SimulatorController.class})
@RequiredArgsConstructor
public class PreferencesControllerAdvice {

    private final GuestPreferencesService guestPreferencesService;
    private final UserService userService;
    private final UserRepository userRepository;

    @ModelAttribute
    public void exposePreferences(HttpSession session, HttpServletRequest request,
                                  Authentication authentication, Model model) {
        model.addAttribute("currentPath", request.getServletPath());
        boolean authenticated = AuthenticationHelper.isAuthenticated(authentication);
        PreferencesRequest preferences = authenticated
                ? userRepository.findByUsername(authentication.getName())
                        .map(user -> userService.getPreferencesSnapshot(user.getId()))
                        .orElseGet(() -> guestPreferencesService.read(session))
                : guestPreferencesService.read(session);

        model.addAttribute("theme", preferences.getThemeStyle());
        model.addAttribute("darkMode", preferences.isDarkMode());
        model.addAttribute("reduceMotion", preferences.isReduceMotion());
        model.addAttribute("fontScale", preferences.getFontScale());
        model.addAttribute("authenticated", authenticated);
    }
}
