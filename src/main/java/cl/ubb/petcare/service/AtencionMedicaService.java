package cl.ubb.petcare.service;

import cl.ubb.petcare.dto.*;
import cl.ubb.petcare.model.*;
import cl.ubb.petcare.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AtencionMedicaService {

    private final AtencionMedicaRepository atencionRepository;
    private final MedicamentoPrescritoRepository medicamentoRepository;
    private final ReservaRepository reservaRepository;
    private final MascotaRepository mascotaRepository;

    public AtencionMedicaService(
            AtencionMedicaRepository atencionRepository,
            MedicamentoPrescritoRepository medicamentoRepository,
            ReservaRepository reservaRepository,
            MascotaRepository mascotaRepository) {

        this.atencionRepository = atencionRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.reservaRepository = reservaRepository;
        this.mascotaRepository = mascotaRepository;
    }

    // RF-09: Registrar una atencion medica
    @Transactional
    public AtencionResponse registrar(
            Long mascotaId,
            CrearAtencionRequest datos) {

        Reserva reserva = reservaRepository
                .bloquearPorId(datos.reservaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reserva no encontrada"
                ));

        // Comprobar que la reserva pertenece a la mascota.
        if (!reserva.getMascota().getId().equals(mascotaId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La reserva no corresponde a esta mascota"
            );
        }

        // Comprobar que corresponde al veterinario indicado.
        if (!reserva.getVeterinario().getId()
                .equals(datos.veterinarioId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El veterinario no corresponde a la reserva"
            );
        }

        // No registrar atenciones de reservas sin confirmar
        // ni de reservas canceladas.
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA
                && reserva.getEstado() != EstadoReserva.ATENDIDA) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva debe estar confirmada o atendida"
            );
        }

        LocalDateTime fechaReserva = LocalDateTime.of(
                reserva.getFecha(),
                reserva.getHora()
        );

        if (fechaReserva.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede registrar una atencion futura"
            );
        }

        // Una reserva solo puede tener una atencion medica.
        if (atencionRepository
                .findByReserva_Id(reserva.getId())
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La atencion de esta reserva ya fue registrada"
            );
        }

        AtencionMedica atencion = new AtencionMedica();

        atencion.setReserva(reserva);
        atencion.setMascota(reserva.getMascota());
        atencion.setVeterinario(reserva.getVeterinario());
        atencion.setFechaAtencion(LocalDateTime.now());
        atencion.setDiagnostico(datos.diagnostico().trim());
        atencion.setTratamiento(datos.tratamiento().trim());

        AtencionMedica guardada =
                atencionRepository.saveAndFlush(atencion);

        // Guardar los medicamentos asociados a la atencion.
        List<MedicamentoPrescrito> medicamentos =
                new ArrayList<>();

        if (datos.medicamentos() != null) {

            for (MedicamentoPrescritoRequest dato :
                    datos.medicamentos()) {

                MedicamentoPrescrito medicamento =
                        new MedicamentoPrescrito();

                medicamento.setAtencion(guardada);
                medicamento.setNombreMedicamento(
                        dato.nombre().trim()
                );
                medicamento.setDosis(dato.dosis().trim());
                medicamento.setFrecuencia(
                        dato.frecuencia().trim()
                );
                medicamento.setDuracionDias(
                        dato.duracionDias()
                );
                medicamento.setIndicaciones(
                        dato.indicaciones()
                );

                medicamentos.add(medicamento);
            }
        }

        medicamentoRepository.saveAll(medicamentos);

        // Completar la reserva como atendida.
        reserva.setEstado(EstadoReserva.ATENDIDA);
        reservaRepository.save(reserva);

        return convertir(guardada);
    }

    // RF-10: Historial clinico de una mascota
    @Transactional(readOnly = true)
    public List<AtencionResponse> historial(Long mascotaId) {

        if (!mascotaRepository.existsById(mascotaId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mascota no encontrada"
            );
        }

        return atencionRepository
                .findByMascota_IdOrderByFechaAtencionDesc(
                        mascotaId
                )
                .stream()
                .map(this::convertir)
                .toList();
    }

    // Consultar una atencion especifica
    @Transactional(readOnly = true)
    public AtencionResponse obtener(Long id) {

        AtencionMedica atencion = atencionRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Atencion medica no encontrada"
                ));

        return convertir(atencion);
    }

    // Convertir las entidades a DTO de respuesta
    private AtencionResponse convertir(AtencionMedica atencion) {

        List<MedicamentoPrescritoResponse> medicamentos =
                medicamentoRepository
                        .findByAtencion_IdOrderByIdAsc(
                                atencion.getId()
                        )
                        .stream()
                        .map(m -> new MedicamentoPrescritoResponse(
                                m.getId(),
                                m.getNombreMedicamento(),
                                m.getDosis(),
                                m.getFrecuencia(),
                                m.getDuracionDias(),
                                m.getIndicaciones()
                        ))
                        .toList();

        return new AtencionResponse(
                atencion.getId(),
                atencion.getReserva().getId(),
                atencion.getMascota().getId(),
                atencion.getVeterinario().getId(),
                atencion.getFechaAtencion(),
                atencion.getDiagnostico(),
                atencion.getTratamiento(),
                medicamentos
        );
    }
}
