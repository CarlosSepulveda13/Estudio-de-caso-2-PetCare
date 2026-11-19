package cl.ubb.petcare.controller;

import cl.ubb.petcare.service.ApiExternaService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/externo/ubicacion")
public class UbicacionController {

    private final ApiExternaService apiExternaService;

    public UbicacionController(
            ApiExternaService apiExternaService) {

        this.apiExternaService = apiExternaService;
    }

    @GetMapping
    public Map<?, ?> obtenerUbicacion() {

        return apiExternaService.obtenerUbicacion();
    }
}
