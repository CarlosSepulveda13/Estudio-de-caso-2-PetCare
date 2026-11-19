package cl.ubb.petcare;

import cl.ubb.petcare.dto.*;
import cl.ubb.petcare.model.*;
import cl.ubb.petcare.repository.*;
import cl.ubb.petcare.service.ReservaEstadoCambiadoEvento;
import cl.ubb.petcare.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {
    @Mock ReservaRepository reservas;
    @Mock MascotaRepository mascotas;
    @Mock VeterinarioRepository veterinarios;
    @Mock HorarioVeterinarioRepository horarios;
    @Mock ApplicationEventPublisher eventos;
    @InjectMocks ReservaService servicio;

    private Cliente cliente;
    private Mascota mascota;
    private Veterinario veterinario;
    private HorarioVeterinario horario;
    private LocalDate lunes;

    @BeforeEach
    void preparar() {
        lunes = LocalDate.now().plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", 1L);
        cliente.setCorreo("cliente@ejemplo.cl");

        mascota = new Mascota();
        ReflectionTestUtils.setField(mascota, "id", 2L);
        mascota.setCliente(cliente);

        veterinario = new Veterinario();
        ReflectionTestUtils.setField(veterinario, "id", 3L);

        horario = new HorarioVeterinario();
        horario.setVeterinario(veterinario);
        horario.setDiaSemana(DayOfWeek.MONDAY);
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(11, 0));
    }

    private CrearReservaRequest solicitud(LocalTime hora) {
        return new CrearReservaRequest(1L, 2L, 3L, lunes, hora, " Control general ");
    }

    private void accesoValido() {
        when(mascotas.findById(2L)).thenReturn(Optional.of(mascota));
        when(veterinarios.bloquearPorId(3L)).thenReturn(Optional.of(veterinario));
    }

    private void horarioDisponible() {
        when(horarios.findByVeterinario_IdAndActivoTrueOrderByIdAsc(3L))
                .thenReturn(List.of(horario));
    }

    private void devolverEntidadGuardada() {
        when(reservas.save(any(Reserva.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Reserva reservaExistente(EstadoReserva estado) {
        Reserva r = new Reserva();
        ReflectionTestUtils.setField(r, "id", 10L);
        r.setMascota(mascota);
        r.setVeterinario(veterinario);
        r.setFecha(lunes);
        r.setHora(LocalTime.of(9, 0));
        r.setMotivo("Control general");
        r.setEstado(estado);
        return r;
    }

    @Test
    void registraReservaPendiente() {
        accesoValido();
        horarioDisponible();
        devolverEntidadGuardada();

        ReservaResponse respuesta = servicio.registrar(solicitud(LocalTime.of(9, 0)));

        assertEquals(EstadoReserva.PENDIENTE, respuesta.estado());
        assertEquals(2L, respuesta.mascotaId());
        assertEquals("Control general", respuesta.motivo());
        verify(reservas).save(any(Reserva.class));
    }

    @Test
    void rechazaMascotaQueNoPerteneceAlCliente() {
        when(mascotas.findById(2L)).thenReturn(Optional.of(mascota));
        CrearReservaRequest incorrecta = new CrearReservaRequest(
                99L, 2L, 3L, lunes, LocalTime.of(9, 0), "Control");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(incorrecta));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaMascotaInactiva() {
        mascota.setActivo(false);
        when(mascotas.findById(2L)).thenReturn(Optional.of(mascota));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(solicitud(LocalTime.of(9, 0))));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void rechazaHoraQueNoComienzaEnMediaHora() {
        accesoValido();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(solicitud(LocalTime.of(9, 15))));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaHoraFueraDeJornada() {
        accesoValido();
        horarioDisponible();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(solicitud(LocalTime.of(12, 0))));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void evitaReservaDuplicada() {
        accesoValido();
        horarioDisponible();
        when(reservas.existsByVeterinario_IdAndFechaAndHoraAndEstadoIn(
                eq(3L), eq(lunes), eq(LocalTime.of(9, 0)), anyCollection()))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(solicitud(LocalTime.of(9, 0))));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(reservas, never()).save(any());
    }

    @Test
    void disponibilidadExcluyeHorasOcupadas() {
        when(veterinarios.findById(3L)).thenReturn(Optional.of(veterinario));
        horarioDisponible();
        Reserva ocupada = reservaExistente(EstadoReserva.CONFIRMADA);
        when(reservas.findByVeterinario_IdAndFechaAndEstadoIn(
                eq(3L), eq(lunes), anyCollection()))
                .thenReturn(List.of(ocupada));

        DisponibilidadResponse resultado = servicio.disponibilidad(3L, lunes);

        assertEquals(3, resultado.horasDisponibles().size());
        assertFalse(resultado.horasDisponibles().contains(LocalTime.of(9, 0)));
        assertTrue(resultado.horasDisponibles().contains(LocalTime.of(9, 30)));
    }

    @Test
    void noPermiteConsultarDisponibilidadPasada() {
        when(veterinarios.findById(3L)).thenReturn(Optional.of(veterinario));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.disponibilidad(3L, LocalDate.now().minusDays(1)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void confirmarReservaPublicaEvento() {
        Reserva reserva = reservaExistente(EstadoReserva.PENDIENTE);
        when(reservas.bloquearPorId(10L)).thenReturn(Optional.of(reserva));
        devolverEntidadGuardada();

        ReservaResponse resultado = servicio.cambiarEstado(10L,
                new CambiarEstadoReservaRequest(EstadoReserva.CONFIRMADA));

        assertEquals(EstadoReserva.CONFIRMADA, resultado.estado());
        verify(eventos).publishEvent(any(ReservaEstadoCambiadoEvento.class));
    }

    @Test
    void cancelarReservaPublicaEvento() {
        Reserva reserva = reservaExistente(EstadoReserva.CONFIRMADA);
        when(reservas.bloquearPorId(10L)).thenReturn(Optional.of(reserva));
        devolverEntidadGuardada();

        ReservaResponse resultado = servicio.cambiarEstado(10L,
                new CambiarEstadoReservaRequest(EstadoReserva.CANCELADA));

        assertEquals(EstadoReserva.CANCELADA, resultado.estado());
        verify(eventos).publishEvent(any(ReservaEstadoCambiadoEvento.class));
    }

    @Test
    void impideConfirmarDosVeces() {
        when(reservas.bloquearPorId(10L))
                .thenReturn(Optional.of(reservaExistente(EstadoReserva.CONFIRMADA)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.cambiarEstado(10L,
                        new CambiarEstadoReservaRequest(EstadoReserva.CONFIRMADA)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(eventos);
    }

    @Test
    void reservaAtendidaNoSePuedeCancelar() {
        when(reservas.bloquearPorId(10L))
                .thenReturn(Optional.of(reservaExistente(EstadoReserva.ATENDIDA)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.cambiarEstado(10L,
                        new CambiarEstadoReservaRequest(EstadoReserva.CANCELADA)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void reprogramarReservaConfirmadaLaDejaPendiente() {
        Reserva reserva = reservaExistente(EstadoReserva.CONFIRMADA);
        when(veterinarios.bloquearPorId(3L)).thenReturn(Optional.of(veterinario));
        when(reservas.bloquearPorId(10L)).thenReturn(Optional.of(reserva));
        horarioDisponible();
        devolverEntidadGuardada();

        ReservaResponse resultado = servicio.reprogramar(10L,
                new ActualizarReservaRequest(3L, lunes, LocalTime.of(9, 30)));

        assertEquals(EstadoReserva.PENDIENTE, resultado.estado());
        assertEquals(LocalTime.of(9, 30), resultado.hora());
        verify(reservas).existsByVeterinario_IdAndFechaAndHoraAndEstadoInAndIdNot(
                eq(3L), eq(lunes), eq(LocalTime.of(9, 30)), anyCollection(), eq(10L));
    }
}
