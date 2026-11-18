package cl.ubb.petcare.dto;

import cl.ubb.petcare.model.EstadoReserva;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaResponse(
        Long id,
        Long clienteId,
        Long mascotaId,
        Long veterinarioId,
        LocalDate fecha,
        LocalTime hora,
        String motivo,
        EstadoReserva estado
) {
}
