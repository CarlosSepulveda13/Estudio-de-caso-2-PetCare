package cl.ubb.petcare.service;

import cl.ubb.petcare.dto.VeterinarioRequest;
import cl.ubb.petcare.model.Veterinario;
import cl.ubb.petcare.repository.VeterinarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class VeterinarioService {

    private final VeterinarioRepository veterinarioRepository;

    public VeterinarioService(
            VeterinarioRepository veterinarioRepository) {
        this.veterinarioRepository = veterinarioRepository;
    }

    @Transactional
    public Veterinario registrar(VeterinarioRequest datos) {

        String correo = normalizarCorreo(datos.correo());

        if (veterinarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        Veterinario veterinario = new Veterinario();

        veterinario.setNombre(datos.nombre().trim());
        veterinario.setEspecialidad(datos.especialidad().trim());
        veterinario.setCorreo(correo);
        veterinario.setTelefono(datos.telefono());

        return veterinarioRepository.save(veterinario);
    }

    @Transactional(readOnly = true)
    public List<Veterinario> listar() {
        return veterinarioRepository.findByActivoTrueOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Veterinario obtener(Long id) {
        return buscarActivo(id);
    }

    @Transactional
    public Veterinario actualizar(
            Long id, VeterinarioRequest datos) {

        Veterinario veterinario = buscarActivo(id);
        String correo = normalizarCorreo(datos.correo());

        if (!veterinario.getCorreo().equalsIgnoreCase(correo)
                && veterinarioRepository.existsByCorreoIgnoreCase(correo)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya esta registrado"
            );
        }

        veterinario.setNombre(datos.nombre().trim());
        veterinario.setEspecialidad(datos.especialidad().trim());
        veterinario.setCorreo(correo);
        veterinario.setTelefono(datos.telefono());

        return veterinarioRepository.save(veterinario);
    }

    @Transactional
    public void desactivar(Long id) {
        Veterinario veterinario = buscarActivo(id);
        veterinario.setActivo(false);
        veterinarioRepository.save(veterinario);
    }

    private Veterinario buscarActivo(Long id) {
        return veterinarioRepository.findById(id)
                .filter(Veterinario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Veterinario no encontrado"
                ));
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}

