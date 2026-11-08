package br.edu.recursos.integracao;

import br.edu.recursos.dominio.StatusReserva;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


@Component
public class NotificacaoCliente {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoCliente.class);

    private final RestClient http;

    public NotificacaoCliente(RestClient.Builder construtor,
                              @Value("${app.notificacao.url}") String urlBase,
                              @Value("${app.notificacao.timeout-ms}") long timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(timeoutMs));
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.http = construtor.baseUrl(urlBase).requestFactory(fabrica).build();
    }

    /** Retorna true se o serviço externo aceitou a notificação. */
    public boolean notificarDecisao(String destinatario, UUID reservaId, UUID recursoId, StatusReserva status) {
        try {
            http.post()
                    .uri("/notificacoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Notificacao(destinatario, reservaId, recursoId, status.name()))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            LOG.warn("Falha ao notificar a reserva {}: {}", reservaId, e.getMessage());
            return false;
        }
    }

    public record Notificacao(String destinatario, UUID reservaId, UUID recursoId, String status) {
    }
}