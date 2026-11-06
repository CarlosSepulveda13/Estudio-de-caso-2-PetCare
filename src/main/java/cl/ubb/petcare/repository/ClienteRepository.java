package cl.ubb.petcare.repository;

import cl.ubb.petcare.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByActivoTrueOrderByIdAsc();

    boolean existsByCorreoIgnoreCase(String correo);
}

