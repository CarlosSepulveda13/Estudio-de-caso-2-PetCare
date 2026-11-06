package cl.ubb.petcare.service;

import cl.ubb.petcare.dto.ClienteRequest;
import cl.ubb.petcare.model.Cliente;
import cl.ubb.petcare.repository.ClienteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente registrar(ClienteRequest datos) {
        String correo = normalizarCorreo(datos.correo());

        if (clienteRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
            );
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(datos.nombre().trim());
        cliente.setCorreo(correo);
        cliente.setTelefono(datos.telefono());

        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findByActivoTrueOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return buscarActivo(id);
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteRequest datos) {
        Cliente cliente = buscarActivo(id);
        String correo = normalizarCorreo(datos.correo());

        if (!cliente.getCorreo().equalsIgnoreCase(correo)
                && clienteRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
            );
        }

        cliente.setNombre(datos.nombre().trim());
        cliente.setCorreo(correo);
        cliente.setTelefono(datos.telefono());

        return clienteRepository.save(cliente);
    }

    @Transactional
    public void desactivar(Long id) {
        Cliente cliente = buscarActivo(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private Cliente buscarActivo(Long id) {
        return clienteRepository.findById(id)
                .filter(Cliente::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado"
                ));
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}

