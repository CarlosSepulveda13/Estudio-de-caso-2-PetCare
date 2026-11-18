package cl.ubb.petcare.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record ActualizarReservaRequest(

        @NotNull @Positive
        Long veterinarioId,

        @NotNull
        LocalDate fecha,

        @NotNull
        LocalTime hora
) {
}
