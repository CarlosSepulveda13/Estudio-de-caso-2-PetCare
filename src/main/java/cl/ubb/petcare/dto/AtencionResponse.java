package cl.ubb.petcare.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AtencionResponse(
        Long id,
        Long reservaId,
        Long mascotaId,
        Long veterinarioId,
        LocalDateTime fechaAtencion,
        String diagnostico,
        String tratamiento,
        List<MedicamentoPrescritoResponse> medicamentos
) {
}
