package cl.ubb.petcare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

@Component
public class NotificacionReservaListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificacionReservaListener.class
            );

    private final RestClient clienteHttp;
    private final String apiKey;

    public NotificacionReservaListener(
            @Value("${petcare.api-externa.url}") String url,
            @Value("${petcare.api-externa.key:}") String apiKey) {

        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));

        this.clienteHttp = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void notificar(
            ReservaEstadoCambiadoEvento evento) {

        if (apiKey.isBlank()) {
            log.warn(
                    "Notificacion omitida: API_KEY no configurada"
            );
            return;
        }

        Map<String, Object> datos = Map.of(
                "destinatario", evento.destinatario(),
                "tipo", evento.tipo(),
                "reservaId", evento.reservaId(),
                "fecha", evento.fecha().toString(),
                "hora", evento.hora().toString()
        );

        try {
            clienteHttp.post()
                    .uri("/api/notificaciones")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(datos)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "API externa proceso notificacion {} de reserva {}",
                    evento.tipo(),
                    evento.reservaId()
            );

        } catch (RestClientException e) {

            log.warn(
                    "No se pudo notificar la reserva {}: {}",
                    evento.reservaId(),
                    e.getMessage()
            );
        }
    }
}
