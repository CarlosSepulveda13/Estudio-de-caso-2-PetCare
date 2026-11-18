package cl.ubb.petcare.service;

import cl.ubb.petcare.dto.*;
import cl.ubb.petcare.model.*;
import cl.ubb.petcare.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private static final int DURACION_MINUTOS = 30;

    private static final Set<EstadoReserva> ESTADOS_OCUPADOS =
            Set.of(
                    EstadoReserva.PENDIENTE,
                    EstadoReserva.CONFIRMADA,
                    EstadoReserva.ATENDIDA
            );

    private final ReservaRepository reservaRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final HorarioVeterinarioRepository horarioRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            MascotaRepository mascotaRepository,
            VeterinarioRepository veterinarioRepository,
            HorarioVeterinarioRepository horarioRepository) {

        this.reservaRepository = reservaRepository;
        this.mascotaRepository = mascotaRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.horarioRepository = horarioRepository;
    }

    // RF-05 y RF-06: Registrar reserva sin duplicaciones
    @Transactional
    public ReservaResponse registrar(CrearReservaRequest datos) {

        Mascota mascota = mascotaRepository
                .findById(datos.mascotaId())
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Mascota no encontrada"
                ));

        if (!mascota.isActivo() || !mascota.getCliente().isActivo()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Mascota o cliente inactivo"
            );
        }

        if (!mascota.getCliente().getId().equals(datos.clienteId())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "La mascota no pertenece al cliente indicado"
            );
        }

        // El bloqueo impide reservas simultaneas del mismo profesional.
        Veterinario veterinario = bloquearVeterinario(
                datos.veterinarioId()
        );

        validarCita(veterinario, datos.fecha(), datos.hora());
        verificarHoraLibre(
                veterinario.getId(),
                datos.fecha(),
                datos.hora(),
                null
        );

        Reserva reserva = new Reserva();
        reserva.setMascota(mascota);
        reserva.setVeterinario(veterinario);
        reserva.setFecha(datos.fecha());
        reserva.setHora(datos.hora());
        reserva.setMotivo(datos.motivo().trim());
        reserva.setEstado(EstadoReserva.PENDIENTE);

        return convertir(reservaRepository.save(reserva));
    }

    // RF-04: Obtener disponibilidad real por fecha
    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(
            Long veterinarioId, LocalDate fecha) {

        Veterinario veterinario = veterinarioRepository
                .findById(veterinarioId)
                .filter(Veterinario::isActivo)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Veterinario no encontrado"
                ));

        if (fecha.isBefore(LocalDate.now())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "No se pueden consultar fechas pasadas"
            );
        }

        Set<LocalTime> horasOcupadas = reservaRepository
                .findByVeterinario_IdAndFechaAndEstadoIn(
                        veterinarioId, fecha, ESTADOS_OCUPADOS
                )
                .stream()
                .map(Reserva::getHora)
                .collect(Collectors.toSet());

        Set<LocalTime> disponibles = new TreeSet<>();

        List<HorarioVeterinario> horarios = horarioRepository
                .findByVeterinario_IdAndActivoTrueOrderByIdAsc(
                        veterinario.getId()
                );

        int bloqueSegundos = DURACION_MINUTOS * 60;

        for (HorarioVeterinario horario : horarios) {

            if (horario.getDiaSemana() != fecha.getDayOfWeek()) {
                continue;
            }

            int inicio = horario.getHoraInicio().toSecondOfDay();
            int fin = horario.getHoraFin().toSecondOfDay();

            // Ajustar al siguiente bloque de 30 minutos.
            int primerBloque =
                    ((inicio + bloqueSegundos - 1) / bloqueSegundos)
                            * bloqueSegundos;

            for (int segundo = primerBloque;
                 segundo + bloqueSegundos <= fin;
                 segundo += bloqueSegundos) {

                LocalTime hora = LocalTime.ofSecondOfDay(segundo);

                if (!horasOcupadas.contains(hora)
                        && LocalDateTime.of(fecha, hora)
                        .isAfter(LocalDateTime.now())) {

                    disponibles.add(hora);
                }
            }
        }

        return new DisponibilidadResponse(
                veterinarioId,
                fecha,
                new ArrayList<>(disponibles)
        );
    }

    // RF-07: Reprogramar reserva
    @Transactional
    public ReservaResponse reprogramar(
            Long id, ActualizarReservaRequest datos) {

        // Bloquear primero el veterinario de destino.
        Veterinario veterinario = bloquearVeterinario(
                datos.veterinarioId()
        );

        // Luego bloquear la reserva para evitar cambios simultaneos.
        Reserva reserva = bloquearReserva(id);

        if (reserva.getEstado() != EstadoReserva.PENDIENTE
                && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw error(
                    HttpStatus.CONFLICT,
                    "La reserva no se puede reprogramar"
            );
        }

        boolean sinCambios =
                reserva.getVeterinario().getId()
                        .equals(datos.veterinarioId())
                        && reserva.getFecha().equals(datos.fecha())
                        && reserva.getHora().equals(datos.hora());

        if (sinCambios) {
            return convertir(reserva);
        }

        validarCita(veterinario, datos.fecha(), datos.hora());

        // Excluir la propia reserva del control de duplicados.
        verificarHoraLibre(
                veterinario.getId(),
                datos.fecha(),
                datos.hora(),
                reserva.getId()
        );

        reserva.setVeterinario(veterinario);
        reserva.setFecha(datos.fecha());
        reserva.setHora(datos.hora());
        reserva.setEstado(EstadoReserva.PENDIENTE);

        return convertir(reservaRepository.save(reserva));
    }

    // RF-08: Gestionar estados
    @Transactional
    public ReservaResponse cambiarEstado(
            Long id, CambiarEstadoReservaRequest datos) {

        Reserva reserva = bloquearReserva(id);

        EstadoReserva actual = reserva.getEstado();
        EstadoReserva nuevo = datos.estado();

        boolean permitido = switch (nuevo) {
            case CONFIRMADA -> actual == EstadoReserva.PENDIENTE;
            case CANCELADA -> actual == EstadoReserva.PENDIENTE
                    || actual == EstadoReserva.CONFIRMADA;
            case ATENDIDA -> actual == EstadoReserva.CONFIRMADA;
            case PENDIENTE -> false;
        };

        if (!permitido) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Cambio de estado no permitido"
            );
        }

        if (nuevo == EstadoReserva.CONFIRMADA
                && !LocalDateTime.of(
                reserva.getFecha(), reserva.getHora()
        ).isAfter(LocalDateTime.now())) {

            throw error(
                    HttpStatus.CONFLICT,
                    "No se puede confirmar una reserva pasada"
            );
        }

        if (nuevo == EstadoReserva.ATENDIDA
                && LocalDateTime.of(
                reserva.getFecha(), reserva.getHora()
        ).isAfter(LocalDateTime.now())) {

            throw error(
                    HttpStatus.CONFLICT,
                    "La atencion aun no ha comenzado"
            );
        }

        reserva.setEstado(nuevo);

        // La notificacion externa se integrara en el bloque de APIs.
        return convertir(reservaRepository.save(reserva));
    }

    // Consultas de reservas
    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id) {
        return convertir(buscarReserva(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(
            Long clienteId, LocalDate fecha) {

        List<Reserva> reservas;

        if (clienteId != null && fecha != null) {
            reservas = reservaRepository
                    .findByMascota_Cliente_IdAndFechaOrderByFechaDescHoraDesc(
                            clienteId, fecha
                    );
        } else if (clienteId != null) {
            reservas = reservaRepository
                    .findByMascota_Cliente_IdOrderByFechaDescHoraDesc(
                            clienteId
                    );
        } else if (fecha != null) {
            reservas = reservaRepository
                    .findByFechaOrderByFechaDescHoraDesc(fecha);
        } else {
            reservas = reservaRepository.findAllByOrderByFechaDescHoraDesc();
        }

        return reservas.stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarPorMascota(Long mascotaId) {

        if (!mascotaRepository.existsById(mascotaId)) {
            throw error(
                    HttpStatus.NOT_FOUND, "Mascota no encontrada"
            );
        }

        return reservaRepository
                .findByMascota_IdOrderByFechaDescHoraDesc(mascotaId)
                .stream()
                .map(this::convertir)
                .toList();
    }

    // Validaciones comunes
    private Veterinario bloquearVeterinario(Long id) {

        Veterinario veterinario = veterinarioRepository
                .bloquearPorId(id)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Veterinario no encontrado"
                ));

        if (!veterinario.isActivo()) {
            throw error(
                    HttpStatus.CONFLICT, "Veterinario inactivo"
            );
        }

        return veterinario;
    }

    private void validarCita(
            Veterinario veterinario,
            LocalDate fecha,
            LocalTime hora) {

        if (!LocalDateTime.of(fecha, hora)
                .isAfter(LocalDateTime.now())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "La fecha y hora deben ser futuras"
            );
        }

        if (hora.getMinute() % DURACION_MINUTOS != 0
                || hora.getSecond() != 0
                || hora.getNano() != 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Las horas deben comenzar cada 30 minutos"
            );
        }

        List<HorarioVeterinario> horarios = horarioRepository
                .findByVeterinario_IdAndActivoTrueOrderByIdAsc(
                        veterinario.getId()
                );

        boolean valido = horarios.stream().anyMatch(h ->
                h.getDiaSemana() == fecha.getDayOfWeek()
                        && !hora.isBefore(h.getHoraInicio())
                        && hora.toSecondOfDay() + DURACION_MINUTOS * 60
                        <= h.getHoraFin().toSecondOfDay()
        );

        if (!valido) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Hora fuera del horario del veterinario"
            );
        }
    }

    private void verificarHoraLibre(
            Long veterinarioId,
            LocalDate fecha,
            LocalTime hora,
            Long reservaExcluirId) {

        boolean ocupada;

        if (reservaExcluirId == null) {
            ocupada = reservaRepository
                    .existsByVeterinario_IdAndFechaAndHoraAndEstadoIn(
                            veterinarioId,
                            fecha,
                            hora,
                            ESTADOS_OCUPADOS
                    );
        } else {
            ocupada = reservaRepository
                    .existsByVeterinario_IdAndFechaAndHoraAndEstadoInAndIdNot(
                            veterinarioId,
                            fecha,
                            hora,
                            ESTADOS_OCUPADOS,
                            reservaExcluirId
                    );
        }

        if (ocupada) {
            throw error(
                    HttpStatus.CONFLICT,
                    "El veterinario ya tiene una reserva en esa hora"
            );
        }
    }

    private Reserva buscarReserva(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Reserva no encontrada"
                ));
    }

    private Reserva bloquearReserva(Long id) {
        return reservaRepository.bloquearPorId(id)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND, "Reserva no encontrada"
                ));
    }

    private ReservaResponse convertir(Reserva reserva) {

        return new ReservaResponse(
                reserva.getId(),
                reserva.getMascota().getCliente().getId(),
                reserva.getMascota().getId(),
                reserva.getVeterinario().getId(),
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getMotivo(),
                reserva.getEstado()
        );
    }

    private ResponseStatusException error(
            HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
