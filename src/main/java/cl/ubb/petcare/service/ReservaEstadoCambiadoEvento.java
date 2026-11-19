package cl.ubb.petcare.service;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaEstadoCambiadoEvento(
        Long reservaId,
        String destinatario,
        String tipo,
        LocalDate fecha,
        LocalTime hora
) {
}
