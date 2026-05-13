package com.electoral.results_service.publicacion.health;

import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.calendario.EstadoJornadaProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Instant;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reporta el estado del motor de publicación: estado actual, estado del circuit
 * breaker SR-M1 y timestamp de la última sincronización exitosa.
 *
 * <p>Marca el componente como {@code DOWN} cuando el circuit breaker SR-M1 está
 * abierto (el motor opera en política de fallo seguro y los operadores deben saberlo).
 */
@Component("motorPublicacion")
public class MotorPublicacionHealthIndicator implements HealthIndicator {

    private final EstadoJornadaProvider provider;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public MotorPublicacionHealthIndicator(
            EstadoJornadaProvider provider, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.provider = provider;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        EstadoJornada estado = provider.obtener();
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("srM1");
        Instant ultimaOk = provider.ultimaSincronizacionExitosa();

        Health.Builder builder = (cb.getState() == CircuitBreaker.State.OPEN || estado.isFalloSeguro())
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("estadoMotor", estado.getEstado())
                .withDetail("falloSeguro", estado.isFalloSeguro())
                .withDetail("circuitBreakerSrM1", cb.getState().name())
                .withDetail(
                        "ultimaSincronizacionExitosa",
                        ultimaOk != null ? ultimaOk.toString() : "NUNCA")
                .build();
    }
}
