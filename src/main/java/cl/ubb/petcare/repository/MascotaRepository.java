package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByCliente_IdAndActivoTrueOrderByIdAsc(
            Long clienteId
    );
}
