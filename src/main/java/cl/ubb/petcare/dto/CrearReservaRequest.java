package cl.ubb.petcare.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record CrearReservaRequest(

        @NotNull @Positive
        Long clienteId,

        @NotNull @Positive
        Long mascotaId,

        @NotNull @Positive
        Long veterinarioId,

        @NotNull
        LocalDate fecha,

        @NotNull
        LocalTime hora,

        @NotBlank
        @Size(max = 500)
        String motivo
) {
}
