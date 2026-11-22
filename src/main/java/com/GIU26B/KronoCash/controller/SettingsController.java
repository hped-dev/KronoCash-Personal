package com.GIU26B.KronoCash.controller;

import com.GIU26B.KronoCash.dto.PreferencesRequest;
import com.GIU26B.KronoCash.dto.ProfileDTO;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.repository.UserRepository;
import com.GIU26B.KronoCash.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    public String settingsForm(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("profile", ProfileDTO.from(user));
        model.addAttribute("preferencesRequest",
                UserService.toRequest(userService.getOrCreatePreferences(user.getId())));
        // Easter egg de cumpleaños: decisión tomada solo en el servidor.
        model.addAttribute("isBirthdayUser", isBirthdayUser(authentication));
        return "public/settings";
    }

    @PostMapping("/preferences")
    public String updatePreferences(@Valid @ModelAttribute("preferencesRequest") PreferencesRequest request,
                                    BindingResult bindingResult,
                                    Authentication authentication,
                                    Model model) {
        User user = currentUser(authentication);
        model.addAttribute("profile", ProfileDTO.from(user));
        model.addAttribute("isBirthdayUser", isBirthdayUser(authentication));
        if (bindingResult.hasErrors()) {
            // request ya está en el modelo gracias a @ModelAttribute.
            return "public/settings";
        }
        userService.updatePreferences(user.getId(), request);
        return "redirect:/settings?saved";
    }

    /** Solo el usuario autenticado "Christianzacui" (cualquier capitalización). */
    private boolean isBirthdayUser(Authentication authentication) {
        return authentication != null && authentication.getName() != null
                && authentication.getName().equalsIgnoreCase("Christianzacui");
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
