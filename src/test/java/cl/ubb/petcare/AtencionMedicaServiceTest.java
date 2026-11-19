package cl.ubb.petcare;

import cl.ubb.petcare.dto.*;
import cl.ubb.petcare.model.*;
import cl.ubb.petcare.repository.*;
import cl.ubb.petcare.service.AtencionMedicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtencionMedicaServiceTest {
    @Mock AtencionMedicaRepository atenciones;
    @Mock MedicamentoPrescritoRepository medicamentos;
    @Mock ReservaRepository reservas;
    @Mock MascotaRepository mascotas;
    @InjectMocks AtencionMedicaService servicio;

    private Mascota mascota;
    private Veterinario veterinario;

    @BeforeEach
    void preparar() {
        mascota = new Mascota();
        ReflectionTestUtils.setField(mascota, "id", 2L);

        veterinario = new Veterinario();
        ReflectionTestUtils.setField(veterinario, "id", 3L);
    }

    private Reserva reserva(EstadoReserva estado, LocalDate fecha) {
        Reserva r = new Reserva();
        ReflectionTestUtils.setField(r, "id", 10L);
        r.setMascota(mascota);
        r.setVeterinario(veterinario);
        r.setFecha(fecha);
        r.setHora(LocalTime.of(9, 0));
        r.setEstado(estado);
        r.setMotivo("Control");
        return r;
    }

    private CrearAtencionRequest solicitud() {
        return new CrearAtencionRequest(10L, 3L,
                "  Diagnóstico de prueba  ",
                "  Tratamiento de prueba  ",
                List.of(new MedicamentoPrescritoRequest(
                        "Medicamento de prueba", "1 comprimido", "Cada 12 horas",
                        3, "Solo para demostración")));
    }

    @Test
    void registraAtencionYMedicamentoYMarcaReservaAtendida() {
        Reserva reserva = reserva(EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1));
        when(reservas.bloquearPorId(10L)).thenReturn(Optional.of(reserva));
        when(atenciones.saveAndFlush(any(AtencionMedica.class))).thenAnswer(invocacion -> {
            AtencionMedica a = invocacion.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 20L);
            return a;
        });
        when(medicamentos.saveAll(anyList())).thenAnswer(invocacion -> {
            List<MedicamentoPrescrito> lista = invocacion.getArgument(0);
            assertEquals(1, lista.size());
            assertEquals("Medicamento de prueba", lista.get(0).getNombreMedicamento());
            assertEquals(3, lista.get(0).getDuracionDias());
            return lista;
        });

        AtencionResponse respuesta = servicio.registrar(2L, solicitud());

        assertEquals("Diagnóstico de prueba", respuesta.diagnostico());
        assertEquals(EstadoReserva.ATENDIDA, reserva.getEstado());
        verify(reservas).save(reserva);
        verify(medicamentos).saveAll(anyList());
    }

    @Test
    void noPermiteAtenderReservaPendiente() {
        when(reservas.bloquearPorId(10L))
                .thenReturn(Optional.of(reserva(EstadoReserva.PENDIENTE, LocalDate.now().minusDays(1))));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(2L, solicitud()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(atenciones, never()).saveAndFlush(any());
    }

    @Test
    void noPermiteAtenderReservaFutura() {
        when(reservas.bloquearPorId(10L))
                .thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(3))));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(2L, solicitud()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void noPermiteRegistrarAtencionConOtraMascota() {
        when(reservas.bloquearPorId(10L))
                .thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1))));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(99L, solicitud()));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void noPermiteRegistrarConVeterinarioDistinto() {
        when(reservas.bloquearPorId(10L))
                .thenReturn(Optional.of(reserva(EstadoReserva.CONFIRMADA, LocalDate.now().minusDays(1))));

        CrearAtencionRequest incorrecta = new CrearAtencionRequest(
                10L, 99L, "Diagnóstico", "Tratamiento", List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(2L, incorrecta));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void impideDosAtencionesParaLaMismaReserva() {
        when(reservas.bloquearPorId(10L))
                .thenReturn(Optional.of(reserva(EstadoReserva.ATENDIDA, LocalDate.now().minusDays(1))));
        when(atenciones.findByReserva_Id(10L))
                .thenReturn(Optional.of(new AtencionMedica()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(2L, solicitud()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(atenciones, never()).saveAndFlush(any());
    }

    @Test
    void historialDeMascotaInexistenteDevuelve404() {
        when(mascotas.existsById(999L)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.historial(999L));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
