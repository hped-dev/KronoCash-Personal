package com.GIU26B.KronoCash.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pantalla de bienvenida: selector de tema para la primera visita. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
