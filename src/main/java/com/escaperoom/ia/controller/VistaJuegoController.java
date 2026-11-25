package com.escaperoom.ia.controller;

import com.escaperoom.ia.model.Idioma;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VistaJuegoController {
    @GetMapping("/")
    public String inicio() {
        return "inicio";
    }

    @GetMapping("/juego")
    public String juego(@RequestParam(defaultValue = "ES") Idioma idioma, Model modelo) {
        modelo.addAttribute("idioma", idioma);
        return "juego";
    }
}
