
package com.github.engvxtvictorlgtm.kittrack;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class KitTrackController {

    private final AulaRepository repository;

    public KitTrackController(AulaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("aulas", repository.listarAulas());
        model.addAttribute("transmissores", repository.listarTransmissores());
        model.addAttribute("mensagem", model.getAttribute("mensagem"));
        return "index";
    }

    @PostMapping("/transmissores")
    public String cadastrarTransmissor(
            @RequestParam String nome,
            RedirectAttributes redirect) {

        if (nome == null || nome.isBlank()) {
            redirect.addFlashAttribute(
                "erro", "Informe o nome do transmissor."
            );
            return "redirect:/";
        }

        try {
            repository.cadastrarTransmissor(nome);
            redirect.addFlashAttribute(
                "mensagem", "Transmissor cadastrado com sucesso."
            );
        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "erro", "Não foi possível cadastrar o transmissor."
            );
        }

        return "redirect:/";
    }

    @PostMapping("/aulas")
    public String cadastrarAula(
            @RequestParam String transmissor,
            @RequestParam LocalDate data,
            @RequestParam LocalTime inicio,
            @RequestParam LocalTime fim,
            @RequestParam String sala,
            @RequestParam(defaultValue = "0") int camera,
            @RequestParam(defaultValue = "0") int microfone,
            @RequestParam(defaultValue = "0") int passador,
            @RequestParam(defaultValue = "0") int pincel,
            RedirectAttributes redirect) {

        try {
            repository.cadastrarAula(
                transmissor, data, inicio, fim, sala,
                camera, microfone, passador, pincel
            );

            redirect.addFlashAttribute(
                "mensagem", "Aula cadastrada com sucesso."
            );
        } catch (IllegalArgumentException | DataAccessException e) {
            redirect.addFlashAttribute(
                "erro", "Não foi possível cadastrar a aula. Confira os dados e os itens."
            );
        }

        return "redirect:/";
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
