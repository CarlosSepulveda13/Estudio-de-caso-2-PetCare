package cl.ubb.petcare.controller;

import cl.ubb.petcare.dto.*;
import cl.ubb.petcare.service.ReservaService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // Crear una reserva
    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponse> registrar(
            @Valid @RequestBody CrearReservaRequest datos) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.registrar(datos));
    }

    // Consultar reservas con filtros opcionales
    @GetMapping("/reservas")
    public List<ReservaResponse> listar(
            @RequestParam(required = false) Long clienteId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        return reservaService.listar(clienteId, fecha);
    }

    // Consultar una reserva
    @GetMapping("/reservas/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return reservaService.obtener(id);
    }

    // Reprogramar una reserva
    @PutMapping("/reservas/{id}")
    public ReservaResponse reprogramar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarReservaRequest datos) {

        return reservaService.reprogramar(id, datos);
    }

    // Cambiar el estado de una reserva
    @PatchMapping("/reservas/{id}/estado")
    public ReservaResponse cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoReservaRequest datos) {

        return reservaService.cambiarEstado(id, datos);
    }

    // Consultar reservas de una mascota
    @GetMapping("/mascotas/{mascotaId}/reservas")
    public List<ReservaResponse> listarPorMascota(
            @PathVariable Long mascotaId) {

        return reservaService.listarPorMascota(mascotaId);
    }

    // Consultar disponibilidad del veterinario
    @GetMapping("/veterinarios/{veterinarioId}/disponibilidad")
    public DisponibilidadResponse disponibilidad(
            @PathVariable Long veterinarioId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        return reservaService.disponibilidad(veterinarioId, fecha);
    }
}
