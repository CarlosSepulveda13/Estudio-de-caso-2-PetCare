package cl.ubb.petcare;

import cl.ubb.petcare.dto.MascotaRequest;
import cl.ubb.petcare.dto.MascotaResponse;
import cl.ubb.petcare.model.Cliente;
import cl.ubb.petcare.model.Mascota;
import cl.ubb.petcare.repository.ClienteRepository;
import cl.ubb.petcare.repository.MascotaRepository;
import cl.ubb.petcare.service.MascotaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaServiceTest {
    @Mock MascotaRepository mascotas;
    @Mock ClienteRepository clientes;
    @InjectMocks MascotaService servicio;

    private Cliente cliente() {
        Cliente c = new Cliente();
        ReflectionTestUtils.setField(c, "id", 1L);
        c.setNombre("Ana");
        return c;
    }

    private MascotaRequest solicitud() {
        return new MascotaRequest("  Luna  ", " Perro ", " Mestiza ",
                LocalDate.now().minusYears(2), new BigDecimal("12.50"));
    }

    @Test
    void registraMascotaAsociadaAlCliente() {
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente()));
        when(mascotas.save(any(Mascota.class))).thenAnswer(invocacion -> {
            Mascota m = invocacion.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 10L);
            return m;
        });

        MascotaResponse respuesta = servicio.registrar(1L, solicitud());

        assertEquals(1L, respuesta.clienteId());
        assertEquals("Luna", respuesta.nombre());
        assertEquals("Perro", respuesta.especie());
        assertEquals(new BigDecimal("12.50"), respuesta.peso());
    }

    @Test
    void impideRegistrarConClienteInactivo() {
        Cliente cliente = cliente();
        cliente.setActivo(false);
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.registrar(1L, solicitud()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(mascotas, never()).save(any());
    }

    @Test
    void desactivarMascotaRealizaBorradoLogico() {
        Mascota mascota = new Mascota();
        when(mascotas.findById(5L)).thenReturn(Optional.of(mascota));

        servicio.desactivar(5L);

        assertFalse(mascota.isActivo());
        verify(mascotas).save(mascota);
    }
}
