package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.AtencionMedica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtencionMedicaRepository
        extends JpaRepository<AtencionMedica, Long> {

    List<AtencionMedica>
    findByMascota_IdOrderByFechaAtencionDesc(Long mascotaId);

    Optional<AtencionMedica> findByReserva_Id(Long reservaId);
}

