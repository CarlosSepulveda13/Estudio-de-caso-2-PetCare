package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.Veterinario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository
        extends JpaRepository<Veterinario, Long> {

    List<Veterinario> findByActivoTrueOrderByIdAsc();

    boolean existsByCorreoIgnoreCase(String correo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Veterinario v WHERE v.id = :id")
    Optional<Veterinario> bloquearPorId(@Param("id") Long id);
}

