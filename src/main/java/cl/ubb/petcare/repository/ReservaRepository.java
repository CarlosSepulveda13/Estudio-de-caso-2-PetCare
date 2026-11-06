package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.EstadoReserva;
import cl.ubb.petcare.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    List<Reserva> findByMascota_IdOrderByFechaDescHoraDesc(
            Long mascotaId
    );

    boolean existsByVeterinario_IdAndFechaAndHoraAndEstadoIn(
            Long veterinarioId,
            LocalDate fecha,
            LocalTime hora,
            Collection<EstadoReserva> estados
    );
}

