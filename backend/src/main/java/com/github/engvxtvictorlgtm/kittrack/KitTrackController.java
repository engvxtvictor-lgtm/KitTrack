package com.github.engvxtvictorlgtm.kittrack;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class KitTrackController {

    private final AulaRepository repository;

    public KitTrackController(AulaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("aulas", repository.listarAulas());
        return "index";
    }

    @PostMapping("/kits/{id}/entregar")
    public String entregar(@PathVariable Long id) {
        repository.entregarKit(id);
        return "redirect:/";
    }

    @PostMapping("/kits/{id}/devolver")
    public String devolver(@PathVariable Long id) {
        repository.devolverKit(id);
        return "redirect:/";
    }
}
