package cl.ubb.petcare.dto;

import cl.ubb.petcare.model.EstadoReserva;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoReservaRequest(

        @NotNull
        EstadoReserva estado
) {
}
