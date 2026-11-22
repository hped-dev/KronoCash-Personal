package com.GIU26B.KronoCash.controller;

import com.GIU26B.KronoCash.dto.PreferencesRequest;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.repository.UserRepository;
import com.GIU26B.KronoCash.service.AuthenticationHelper;
import com.GIU26B.KronoCash.service.GuestPreferencesService;
import com.GIU26B.KronoCash.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Endpoint único de sincronización para accessibility.js:
 * - usuario autenticado -> user_preferences (PostgreSQL);
 * - primera visita -> sesión HTTP.
 */
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferencesApiController {

    private final UserService userService;
    private final GuestPreferencesService guestPreferencesService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Map<String, Boolean>> save(@Valid @RequestBody PreferencesRequest request,
                                                      HttpSession session,
                                                      Authentication authentication) {
        if (AuthenticationHelper.isAuthenticated(authentication)) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
            userService.updatePreferences(user.getId(), request);
        } else {
            guestPreferencesService.save(session, request);
        }
        return ResponseEntity.ok(Map.of("saved", true));
    }
}
