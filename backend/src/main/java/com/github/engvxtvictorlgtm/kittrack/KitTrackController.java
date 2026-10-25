
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
    public String inicio(
            @RequestParam(required = false) LocalDate data,
            @RequestParam(required = false) String sala,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String transmissor,
            Model model) {

        model.addAttribute(
            "aulas",
            repository.listarAulas(data, sala, status, transmissor)
        );
        model.addAttribute(
            "transmissores",
            repository.listarTransmissores()
        );

        model.addAttribute("filtroData", data);
        model.addAttribute("filtroSala", sala);
        model.addAttribute("filtroStatus", status);
        model.addAttribute("filtroTransmissor", transmissor);

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
                "erro", "Nao foi possivel cadastrar o transmissor."
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
                "erro",
                "Nao foi possivel cadastrar a aula. Confira os dados e os itens."
            );
        }

        return "redirect:/";
    }

    @PostMapping("/kits/{id}/entregar")
    public String entregar(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        try {
            repository.entregarKit(id);
            redirect.addFlashAttribute(
                "mensagem", "Kit entregue com sucesso."
            );
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "erro", "Erro no banco ao tentar entregar o kit."
            );
        }

        return "redirect:/";
    }

    @PostMapping("/kits/{id}/devolver")
    public String devolver(
            @PathVariable Long id,
            RedirectAttributes redirect) {

        try {
            repository.devolverKit(id);
            redirect.addFlashAttribute(
                "mensagem", "Kit devolvido com sucesso."
            );
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        } catch (DataAccessException e) {
            redirect.addFlashAttribute(
                "erro", "Erro no banco ao tentar devolver o kit."
            );
        }

        return "redirect:/";
    }
}
