package cl.ubb.petcare.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MascotaRequest(
        @NotBlank
        @Size(max = 100)
        String nombre,

        @NotBlank
        @Size(max = 50)
        String especie,

        @NotBlank
        @Size(max = 100)
        String raza,

        @NotNull
        @PastOrPresent
        LocalDate fechaNacimiento,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal peso
) {
}