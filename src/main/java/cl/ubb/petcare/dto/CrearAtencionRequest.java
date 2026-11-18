package cl.ubb.petcare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CrearAtencionRequest(

        @NotNull
        @Positive
        Long reservaId,

        @NotNull
        @Positive
        Long veterinarioId,

        @NotBlank
        @Size(max = 2000)
        String diagnostico,

        @NotBlank
        @Size(max = 4000)
        String tratamiento,

        List<@Valid MedicamentoPrescritoRequest> medicamentos
) {
}
