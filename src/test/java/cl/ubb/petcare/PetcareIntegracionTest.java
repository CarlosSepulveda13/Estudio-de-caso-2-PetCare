package cl.ubb.petcare;

import cl.ubb.petcare.dto.*;
import cl.ubb.petcare.model.*;
import cl.ubb.petcare.repository.*;
import cl.ubb.petcare.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas con JPA real y H2 EN MEMORIA. No modifican datos/petcare.
 * Cada prueba usa @Transactional y se revierte al finalizar.
 */
@SpringBootTest
@Transactional
class PetcareIntegracionTest {
    @Autowired ClienteRepository clientes;
    @Autowired MascotaRepository mascotas;
    @Autowired VeterinarioRepository veterinarios;
    @Autowired HorarioVeterinarioRepository horarios;
    @Autowired ReservaRepository reservas;
    @Autowired ReservaService servicio;

    private Cliente cliente;
    private Mascota mascota;
    private Veterinario veterinario;
    private LocalDate lunes;

    @BeforeEach
    void crearDatos() {
        lunes = LocalDate.now().plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        cliente = new Cliente();
        cliente.setNombre("Cliente Test");
        cliente.setCorreo("test-" + java.util.UUID.randomUUID() + "@ejemplo.cl");
        cliente.setTelefono("912345678");
        cliente = clientes.saveAndFlush(cliente);

        mascota = new Mascota();
        mascota.setNombre("Mascota Test");
        mascota.setEspecie("Perro");
        mascota.setRaza("Mestizo");
        mascota.setFechaNacimiento(LocalDate.now().minusYears(2));
        mascota.setPeso(new BigDecimal("12.50"));
        mascota.setCliente(cliente);
        mascota = mascotas.saveAndFlush(mascota);

        veterinario = new Veterinario();
        veterinario.setNombre("Veterinario Test");
        veterinario.setEspecialidad("Medicina General");
        veterinario.setCorreo("vet-" + java.util.UUID.randomUUID() + "@ejemplo.cl");
        veterinario.setTelefono("987654321");
        veterinario = veterinarios.saveAndFlush(veterinario);

        HorarioVeterinario horario = new HorarioVeterinario();
        horario.setVeterinario(veterinario);
        horario.setDiaSemana(DayOfWeek.MONDAY);
        horario.setHoraInicio(LocalTime.of(9, 0));
        horario.setHoraFin(LocalTime.of(11, 0));
        horarios.saveAndFlush(horario);
    }

    private CrearReservaRequest solicitud() {
        return new CrearReservaRequest(cliente.getId(), mascota.getId(),
                veterinario.getId(), lunes, LocalTime.of(9, 0), "Control de prueba");
    }

    @Test
    void registraYPersisteReservaYLaExcluyeDeDisponibilidad() {
        ReservaResponse respuesta = servicio.registrar(solicitud());

        assertNotNull(respuesta.id());
        assertEquals(EstadoReserva.PENDIENTE, respuesta.estado());
        assertTrue(reservas.findById(respuesta.id()).isPresent());

        DisponibilidadResponse disponibilidad =
                servicio.disponibilidad(veterinario.getId(), lunes);
        assertFalse(disponibilidad.horasDisponibles().contains(LocalTime.of(9, 0)));
        assertTrue(disponibilidad.horasDisponibles().contains(LocalTime.of(9, 30)));
    }

    @Test
    void cancelarReservaLiberaHoraEnBaseDeDatos() {
        ReservaResponse registrada = servicio.registrar(solicitud());

        ReservaResponse cancelada = servicio.cambiarEstado(registrada.id(),
                new CambiarEstadoReservaRequest(EstadoReserva.CANCELADA));

        assertEquals(EstadoReserva.CANCELADA, cancelada.estado());
        assertEquals(EstadoReserva.CANCELADA,
                reservas.findById(registrada.id()).orElseThrow().getEstado());

        DisponibilidadResponse disponibilidad =
                servicio.disponibilidad(veterinario.getId(), lunes);
        assertTrue(disponibilidad.horasDisponibles().contains(LocalTime.of(9, 0)));
    }
}
