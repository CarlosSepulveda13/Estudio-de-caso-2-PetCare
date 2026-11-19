package cl.ubb.petcare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@Service
public class ApiExternaService {

    private final RestClient clienteHttp;

    public ApiExternaService(
            @Value("${petcare.api-externa.url}") String url) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.clienteHttp = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    // RF-11: Consultar medicamentos externos
    public Map<?, ?> buscarMedicamentos(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar un medicamento"
            );
        }

        try {
            return clienteHttp.get()
                    .uri(uri -> uri
                            .path("/api/medicamentos")
                            .queryParam("nombre", nombre.trim())
                            .build())
                    .retrieve()
                    .body(Map.class);

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar la API de medicamentos"
            );
        }
    }

    // RF-12: Consultar ubicacion de la clinica
    public Map<?, ?> obtenerUbicacion() {

        try {
            return clienteHttp.get()
                    .uri("/api/clinica/ubicacion")
                    .retrieve()
                    .body(Map.class);

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar la ubicacion"
            );
        }
    }
}
