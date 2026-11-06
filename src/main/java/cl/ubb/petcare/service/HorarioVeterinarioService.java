package cl.ubb.petcare.service;

import cl.ubb.petcare.dto.HorarioRequest;
import cl.ubb.petcare.dto.HorarioResponse;
import cl.ubb.petcare.model.HorarioVeterinario;
import cl.ubb.petcare.model.Veterinario;
import cl.ubb.petcare.repository.HorarioVeterinarioRepository;
import cl.ubb.petcare.repository.VeterinarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HorarioVeterinarioService {

    private final HorarioVeterinarioRepository horarioRepository;
    private final VeterinarioRepository veterinarioRepository;

    public HorarioVeterinarioService(
            HorarioVeterinarioRepository horarioRepository,
            VeterinarioRepository veterinarioRepository) {

        this.horarioRepository = horarioRepository;
        this.veterinarioRepository = veterinarioRepository;
    }

    @Transactional
    public HorarioResponse registrar(
            Long veterinarioId, HorarioRequest datos) {

        // Bloquear al veterinario durante la asignacion.
        Veterinario veterinario = veterinarioRepository
                .bloquearPorId(veterinarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Veterinario no encontrado"
                ));

        if (!veterinario.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El veterinario esta inactivo"
            );
        }

        // La hora de inicio debe ser menor que la hora de fin.
        if (!datos.horaInicio().isBefore(datos.horaFin())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El horario de inicio debe ser anterior al de fin"
            );
        }

        // Comprobar si existe un horario superpuesto.
        List<HorarioVeterinario> horariosExistentes =
                horarioRepository
                        .findByVeterinario_IdAndActivoTrueOrderByIdAsc(
                                veterinarioId
                        );

        boolean superpuesto = horariosExistentes.stream()
                .anyMatch(h ->
                        h.getDiaSemana() == datos.diaSemana()
                                && h.getHoraInicio().isBefore(datos.horaFin())
                                && h.getHoraFin().isAfter(datos.horaInicio())
                );

        if (superpuesto) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El veterinario ya tiene un horario superpuesto"
            );
        }

        HorarioVeterinario horario = new HorarioVeterinario();

        horario.setVeterinario(veterinario);
        horario.setDiaSemana(datos.diaSemana());
        horario.setHoraInicio(datos.horaInicio());
        horario.setHoraFin(datos.horaFin());

        return convertir(horarioRepository.save(horario));
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listarPorVeterinario(
            Long veterinarioId) {

        Veterinario veterinario = veterinarioRepository
                .findById(veterinarioId)
                .filter(Veterinario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Veterinario no encontrado"
                ));

        return horarioRepository
                .findByVeterinario_IdAndActivoTrueOrderByIdAsc(
                        veterinario.getId()
                )
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional
    public void desactivar(Long id) {

        HorarioVeterinario horario = horarioRepository
                .findById(id)
                .filter(HorarioVeterinario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Horario no encontrado"
                ));

        horario.setActivo(false);
        horarioRepository.save(horario);
    }

    private HorarioResponse convertir(HorarioVeterinario horario) {

        return new HorarioResponse(
                horario.getId(),
                horario.getVeterinario().getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.isActivo()
        );
    }
}

