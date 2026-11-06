package cl.ubb.petcare.service;

import cl.ubb.petcare.dto.MascotaRequest;
import cl.ubb.petcare.dto.MascotaResponse;
import cl.ubb.petcare.model.Cliente;
import cl.ubb.petcare.model.Mascota;
import cl.ubb.petcare.repository.ClienteRepository;
import cl.ubb.petcare.repository.MascotaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final ClienteRepository clienteRepository;

    public MascotaService(
            MascotaRepository mascotaRepository,
            ClienteRepository clienteRepository) {
        this.mascotaRepository = mascotaRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public MascotaResponse registrar(
            Long clienteId, MascotaRequest datos) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado"
                ));

        if (!cliente.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El cliente está inactivo"
            );
        }

        Mascota mascota = new Mascota();
        mascota.setCliente(cliente);
        aplicarDatos(mascota, datos);

        return convertir(mascotaRepository.save(mascota));
    }

    @Transactional(readOnly = true)
    public List<MascotaResponse> listarPorCliente(Long clienteId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado"
                ));

        if (!cliente.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cliente no encontrado"
            );
        }

        return mascotaRepository
                .findByCliente_IdAndActivoTrueOrderByIdAsc(clienteId)
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public MascotaResponse obtener(Long id) {
        return convertir(buscarActiva(id));
    }

    @Transactional
    public MascotaResponse actualizar(Long id, MascotaRequest datos) {
        Mascota mascota = buscarActiva(id);
        aplicarDatos(mascota, datos);

        return convertir(mascotaRepository.save(mascota));
    }

    @Transactional
    public void desactivar(Long id) {
        Mascota mascota = buscarActiva(id);
        mascota.setActivo(false);
        mascotaRepository.save(mascota);
    }

    private Mascota buscarActiva(Long id) {
        return mascotaRepository.findById(id)
                .filter(Mascota::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mascota no encontrada"
                ));
    }

    private void aplicarDatos(Mascota mascota, MascotaRequest datos) {
        mascota.setNombre(datos.nombre().trim());
        mascota.setEspecie(datos.especie().trim());
        mascota.setRaza(datos.raza().trim());
        mascota.setFechaNacimiento(datos.fechaNacimiento());
        mascota.setPeso(datos.peso());
    }

    private MascotaResponse convertir(Mascota mascota) {
        return new MascotaResponse(
                mascota.getId(),
                mascota.getCliente().getId(),
                mascota.getNombre(),
                mascota.getEspecie(),
                mascota.getRaza(),
                mascota.getFechaNacimiento(),
                mascota.getPeso(),
                mascota.isActivo()
        );
    }
}

