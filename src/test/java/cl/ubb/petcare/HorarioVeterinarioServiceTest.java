package cl.ubb.petcare;

import cl.ubb.petcare.dto.HorarioRequest;
import cl.ubb.petcare.dto.HorarioResponse;
import cl.ubb.petcare.model.HorarioVeterinario;
import cl.ubb.petcare.model.Veterinario;
import cl.ubb.petcare.repository.HorarioVeterinarioRepository;
import cl.ubb.petcare.repository.VeterinarioRepository;
import cl.ubb.petcare.service.HorarioVeterinarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioVeterinarioServiceTest {
    @Mock HorarioVeterinarioRepository horarios;
    @Mock VeterinarioRepository veterinarios;
    @InjectMocks HorarioVeterinarioService servicio;

    private Veterinario veterinario;

    @BeforeEach
    void preparar() {
        veterinario = new Veterinario();
        ReflectionTestUtils.setField(veterinario, "id", 1L);
    }

    private HorarioRequest solicitud(int inicio, int fin) {
        return new HorarioRequest(DayOfWeek.MONDAY,
                LocalTime.of(inicio, 0), LocalTime.of(fin, 0));
    }

    private HorarioVeterinario horarioExistente(int inicio, int fin) {
        HorarioVeterinario h = new HorarioVeterinario();
        h.setVeterinario(veterinario);
        h.setDiaSemana(DayOfWeek.MONDAY);
        h.setHoraInicio(LocalTime.of(inicio, 0));
        h.setHoraFin(LocalTime.of(fin, 0));
        return h;
    }

    @Test
    void registraHorarioValido() {
        when(veterinarios.bloquearPorId(1L)).thenReturn(Optional.of(veterinario));
        when(horarios.findByVeterinario_IdAndActivoTrueOrderByIdAsc(1L))
                .thenReturn(List.of());
        when(horarios.save(any(HorarioVeterinario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        HorarioResponse resultado = servicio.registrar(1L, solicitud(9, 13));

        assertEquals(DayOfWeek.MONDAY, resultado.diaSemana());
        assertEquals(LocalTime.of(9, 0), resultado.horaInicio());
        assertEquals(1L, resultado.veterinarioId());
    }

    @Test
    void rechazaHorariosSuperpuestos() {
        when(veterinarios.bloquearPorId(1L)).thenReturn(Optional.of(veterinario));
        when(horarios.findByVeterinario_IdAndActivoTrueOrderByIdAsc(1L))
                .thenReturn(List.of(horarioExistente(9, 13)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(1L, solicitud(12, 15)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(horarios, never()).save(any());
    }

    @Test
    void aceptaHorariosContiguosSinSuperposicion() {
        when(veterinarios.bloquearPorId(1L)).thenReturn(Optional.of(veterinario));
        when(horarios.findByVeterinario_IdAndActivoTrueOrderByIdAsc(1L))
                .thenReturn(List.of(horarioExistente(9, 13)));
        when(horarios.save(any(HorarioVeterinario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        assertDoesNotThrow(() -> servicio.registrar(1L, solicitud(13, 15)));
    }

    @Test
    void rechazaHoraInicioMayorQueFin() {
        when(veterinarios.bloquearPorId(1L)).thenReturn(Optional.of(veterinario));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(1L, solicitud(15, 9)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void impideHorarioParaVeterinarioInactivo() {
        veterinario.setActivo(false);
        when(veterinarios.bloquearPorId(1L)).thenReturn(Optional.of(veterinario));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(1L, solicitud(9, 13)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void desactivarHorarioEsBorradoLogico() {
        HorarioVeterinario horario = horarioExistente(9, 13);
        when(horarios.findById(7L)).thenReturn(Optional.of(horario));

        servicio.desactivar(7L);

        assertFalse(horario.isActivo());
        verify(horarios).save(horario);
    }
}
