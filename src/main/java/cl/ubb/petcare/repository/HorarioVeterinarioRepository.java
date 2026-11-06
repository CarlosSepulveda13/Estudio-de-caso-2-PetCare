package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.HorarioVeterinario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioVeterinarioRepository
        extends JpaRepository<HorarioVeterinario, Long> {

    List<HorarioVeterinario>
    findByVeterinario_IdAndActivoTrueOrderByIdAsc(Long veterinarioId);
}

