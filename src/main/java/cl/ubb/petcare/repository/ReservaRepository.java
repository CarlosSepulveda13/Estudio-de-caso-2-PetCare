package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.EstadoReserva;
import cl.ubb.petcare.model.Reserva;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findAllByOrderByFechaDescHoraDesc();

    List<Reserva> findByMascota_IdOrderByFechaDescHoraDesc(
            Long mascotaId
    );

    List<Reserva> findByMascota_Cliente_IdOrderByFechaDescHoraDesc(
            Long clienteId
    );

    List<Reserva> findByFechaOrderByFechaDescHoraDesc(
            LocalDate fecha
    );

    List<Reserva>
    findByMascota_Cliente_IdAndFechaOrderByFechaDescHoraDesc(
            Long clienteId,
            LocalDate fecha
    );

    List<Reserva> findByVeterinario_IdAndFechaAndEstadoIn(
            Long veterinarioId,
            LocalDate fecha,
            Collection<EstadoReserva> estados
    );

    boolean existsByVeterinario_IdAndFechaAndHoraAndEstadoInAndIdNot(
            Long veterinarioId,
            LocalDate fecha,
            LocalTime hora,
            Collection<EstadoReserva> estados,
            Long id
    );

    boolean existsByVeterinario_IdAndFechaAndHoraAndEstadoIn(
            Long veterinarioId,
            LocalDate fecha,
            LocalTime hora,
            Collection<EstadoReserva> estados
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id")
    Optional<Reserva> bloquearPorId(@Param("id") Long id);
}
