package cl.ubb.petcare.controller;

import cl.ubb.petcare.dto.VeterinarioRequest;
import cl.ubb.petcare.model.Veterinario;
import cl.ubb.petcare.service.VeterinarioService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/veterinarios")
public class VeterinarioController {

    private final VeterinarioService veterinarioService;

    public VeterinarioController(
            VeterinarioService veterinarioService) {
        this.veterinarioService = veterinarioService;
    }

    @PostMapping
    public ResponseEntity<Veterinario> registrar(
            @Valid @RequestBody VeterinarioRequest datos) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(veterinarioService.registrar(datos));
    }

    @GetMapping
    public List<Veterinario> listar() {
        return veterinarioService.listar();
    }

    @GetMapping("/{id}")
    public Veterinario obtener(@PathVariable Long id) {
        return veterinarioService.obtener(id);
    }

    @PutMapping("/{id}")
    public Veterinario actualizar(
            @PathVariable Long id,
            @Valid @RequestBody VeterinarioRequest datos) {

        return veterinarioService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {

        veterinarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
