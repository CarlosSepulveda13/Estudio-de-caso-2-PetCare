package cl.ubb.petcare.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HorarioResponse(
        Long id,
        Long veterinarioId,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo
) {
}

