package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.MedicamentoPrescrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicamentoPrescritoRepository
        extends JpaRepository<MedicamentoPrescrito, Long> {

    List<MedicamentoPrescrito>
    findByAtencion_IdOrderByIdAsc(Long atencionId);
}

