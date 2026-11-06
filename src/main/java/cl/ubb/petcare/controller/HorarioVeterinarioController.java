package cl.ubb.petcare.controller;

import cl.ubb.petcare.dto.HorarioRequest;
import cl.ubb.petcare.dto.HorarioResponse;
import cl.ubb.petcare.service.HorarioVeterinarioService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HorarioVeterinarioController {

    private final HorarioVeterinarioService horarioService;

    public HorarioVeterinarioController(
            HorarioVeterinarioService horarioService) {
        this.horarioService = horarioService;
    }

    @PostMapping("/veterinarios/{veterinarioId}/horarios")
    public ResponseEntity<HorarioResponse> registrar(
            @PathVariable Long veterinarioId,
            @Valid @RequestBody HorarioRequest datos) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(horarioService.registrar(veterinarioId, datos));
    }

    @GetMapping("/veterinarios/{veterinarioId}/horarios")
    public List<HorarioResponse> listar(
            @PathVariable Long veterinarioId) {

        return horarioService.listarPorVeterinario(veterinarioId);
    }

    @DeleteMapping("/horarios/{id}")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long id) {

        horarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}

