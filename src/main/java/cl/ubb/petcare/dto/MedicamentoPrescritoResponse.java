package cl.ubb.petcare.dto;

public record MedicamentoPrescritoResponse(
        Long id,
        String nombre,
        String dosis,
        String frecuencia,
        Integer duracionDias,
        String indicaciones
) {
}
