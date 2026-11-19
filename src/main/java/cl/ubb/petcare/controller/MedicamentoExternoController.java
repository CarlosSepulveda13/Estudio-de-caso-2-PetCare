package cl.ubb.petcare.controller;

import cl.ubb.petcare.service.ApiExternaService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/externo/medicamentos")
public class MedicamentoExternoController {

    private final ApiExternaService apiExternaService;

    public MedicamentoExternoController(
            ApiExternaService apiExternaService) {

        this.apiExternaService = apiExternaService;
    }

    @GetMapping
    public Map<?, ?> buscar(
            @RequestParam String nombre) {

        return apiExternaService.buscarMedicamentos(nombre);
    }
}
