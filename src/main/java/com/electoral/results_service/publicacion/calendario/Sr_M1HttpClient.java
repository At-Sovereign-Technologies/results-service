package com.electoral.results_service.publicacion.calendario;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador HTTP al calendario SR-M1. Protegido por circuit breaker Resilience4j.
 * Ante cualquier fallo (timeout, 5xx, parsing, CB abierto) propaga
 * {@link CalendarioInaccesibleException} para que el provider aplique fallo seguro.
 */
@Component
public class Sr_M1HttpClient implements CalendarioElectoralPort {

    private static final Logger log = LoggerFactory.getLogger(Sr_M1HttpClient.class);

    private static final String PATH = "/api/v1/calendario/estado-jornada";

    private final RestClient restClient;

    public Sr_M1HttpClient(
            @Value("${srm1.base-url}") String baseUrl,
            @Value("${srm1.timeout-ms:500}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    @CircuitBreaker(name = "srM1", fallbackMethod = "consultarFallback")
    public Sr_M1Respuesta consultar() {
        try {
            Sr_M1Respuesta respuesta = restClient.get()
                    .uri(PATH)
                    .retrieve()
                    .body(Sr_M1Respuesta.class);
            if (respuesta == null) {
                throw new CalendarioInaccesibleException("SR-M1 respondió con body vacío");
            }
            return respuesta;
        } catch (RestClientException ex) {
            throw new CalendarioInaccesibleException("Error consultando SR-M1: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unused")
    private Sr_M1Respuesta consultarFallback(Throwable t) {
        log.warn("SR-M1 FALLBACK - circuit-breaker u otro fallo - {}", t.getMessage());
        throw new CalendarioInaccesibleException("SR-M1 inaccesible (fallback)", t);
    }
}
