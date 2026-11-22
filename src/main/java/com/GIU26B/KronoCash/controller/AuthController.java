package com.GIU26B.KronoCash.controller;

import com.GIU26B.KronoCash.dto.RegisterRequest;
import com.GIU26B.KronoCash.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** El POST /login lo gestiona directamente Spring Security. */
    @GetMapping("/login")
    public String loginForm() {
        return "public/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "public/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           Model model) {
        // Errores de campo (no bloqueantes: se anuncian con aria-live en la vista).
        if (userService.existsByUsername(request.getUsername())) {
            bindingResult.rejectValue("username", "username.exists",
                    "Este nombre de usuario ya está registrado.");
        }
        if (userService.existsByEmail(request.getEmail())) {
            bindingResult.rejectValue("email", "email.exists",
                    "Este correo electrónico ya está registrado.");
        }
        if (bindingResult.hasErrors()) {
            return "public/register";
        }

        try {
            userService.register(request);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registrationError", ex.getMessage());
            return "public/register";
        }
        return "redirect:/login?registered";
    }
}
