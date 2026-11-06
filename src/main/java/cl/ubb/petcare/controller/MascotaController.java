package cl.ubb.petcare.controller;

import cl.ubb.petcare.dto.MascotaRequest;
import cl.ubb.petcare.dto.MascotaResponse;
import cl.ubb.petcare.service.MascotaService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    @PostMapping("/clientes/{clienteId}/mascotas")
    public ResponseEntity<MascotaResponse> registrar(
            @PathVariable Long clienteId,
            @Valid @RequestBody MascotaRequest datos) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mascotaService.registrar(clienteId, datos));
    }

    @GetMapping("/clientes/{clienteId}/mascotas")
    public List<MascotaResponse> listarPorCliente(
            @PathVariable Long clienteId) {

        return mascotaService.listarPorCliente(clienteId);
    }

    @GetMapping("/mascotas/{id}")
    public MascotaResponse obtener(@PathVariable Long id) {
        return mascotaService.obtener(id);
    }

    @PutMapping("/mascotas/{id}")
    public MascotaResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MascotaRequest datos) {

        return mascotaService.actualizar(id, datos);
    }

    @DeleteMapping("/mascotas/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        mascotaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}

