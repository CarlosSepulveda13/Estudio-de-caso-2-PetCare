package cl.ubb.petcare;

import cl.ubb.petcare.dto.ClienteRequest;
import cl.ubb.petcare.model.Cliente;
import cl.ubb.petcare.repository.ClienteRepository;
import cl.ubb.petcare.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    @Mock ClienteRepository repositorio;
    @InjectMocks ClienteService servicio;

    private ClienteRequest solicitud() {
        return new ClienteRequest("  Ana Pérez  ", "  ANA@EJEMPLO.CL  ", "912345678");
    }

    @Test
    void registrarNormalizaNombreYCorreo() {
        when(repositorio.save(any(Cliente.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Cliente cliente = servicio.registrar(solicitud());

        assertEquals("Ana Pérez", cliente.getNombre());
        assertEquals("ana@ejemplo.cl", cliente.getCorreo());
        assertTrue(cliente.isActivo());
        verify(repositorio).save(any(Cliente.class));
    }

    @Test
    void noPermiteCorreoDuplicado() {
        when(repositorio.existsByCorreoIgnoreCase("ana@ejemplo.cl"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.registrar(solicitud())
        );
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).save(any());
    }

    @Test
    void desactivarClienteRealizaBorradoLogico() {
        Cliente cliente = new Cliente();
        when(repositorio.findById(1L)).thenReturn(Optional.of(cliente));

        servicio.desactivar(1L);

        assertFalse(cliente.isActivo());
        verify(repositorio).save(cliente);
    }

    @Test
    void clienteInexistenteDevuelve404() {
        when(repositorio.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.obtener(999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
