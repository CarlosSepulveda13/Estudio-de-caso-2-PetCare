package cl.ubb.petcare.controller;

import cl.ubb.petcare.dto.AtencionResponse;
import cl.ubb.petcare.dto.CrearAtencionRequest;
import cl.ubb.petcare.service.AtencionMedicaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AtencionMedicaController {

    private final AtencionMedicaService atencionService;

    public AtencionMedicaController(
            AtencionMedicaService atencionService) {
        this.atencionService = atencionService;
    }

    // RF-09: Registrar atencion medica
    @PostMapping("/mascotas/{mascotaId}/atenciones")
    public ResponseEntity<AtencionResponse> registrar(
            @PathVariable Long mascotaId,
            @Valid @RequestBody CrearAtencionRequest datos) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atencionService.registrar(mascotaId, datos));
    }

    // RF-10: Consultar historial clinico
    @GetMapping("/mascotas/{mascotaId}/historial")
    public List<AtencionResponse> historial(
            @PathVariable Long mascotaId) {

        return atencionService.historial(mascotaId);
    }

    // Consultar una atencion por ID
    @GetMapping("/atenciones/{id}")
    public AtencionResponse obtener(@PathVariable Long id) {

        return atencionService.obtener(id);
    }
}
