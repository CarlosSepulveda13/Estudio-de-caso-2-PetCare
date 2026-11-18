package cl.ubb.petcare.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadResponse(
        Long veterinarioId,
        LocalDate fecha,
        List<LocalTime> horasDisponibles
) {
}
