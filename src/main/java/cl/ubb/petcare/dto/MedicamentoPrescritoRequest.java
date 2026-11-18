package cl.ubb.petcare.dto;

import jakarta.validation.constraints.*;

public record MedicamentoPrescritoRequest(

        @NotBlank
        @Size(max = 150)
        String nombre,

        @NotBlank
        @Size(max = 100)
        String dosis,

        @NotBlank
        @Size(max = 100)
        String frecuencia,

        @NotNull
        @Positive
        Integer duracionDias,

        @Size(max = 500)
        String indicaciones
) {
}
