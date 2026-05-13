package com.electoral.results_service.publicacion.cache;

import com.electoral.results_service.publicacion.Constantes;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Caché in-memory del payload de participación con TTL fijo de 5s (constante).
 *
 * <p><b>Justificación de la estrategia:</b> a TTL=5s la latencia de red de Redis
 * (~1-2ms) y la coordinación inter-réplica añaden costo sin beneficio operativo
 * (cada réplica puede tener su propia copia desfasada 5s; los datos no son críticos
 * para integridad). El TTL es lo bastante corto para que la elasticidad horizontal
 * funcione: con 10k req/s repartidos entre N réplicas, cada réplica recalcula
 * 1 vez cada 5s. El TTL no es configurable en runtime (constante en código).
 */
@Component
public class ParticipacionCache {

    private final AtomicReference<Entrada> cache = new AtomicReference<>();
    private final Clock clock;

    public ParticipacionCache() {
        this(Clock.systemUTC());
    }

    ParticipacionCache(Clock clock) {
        this.clock = clock;
    }

    public PayloadParticipacion obtener(Supplier<PayloadParticipacion> supplier) {
        Instant now = clock.instant();
        Entrada actual = cache.get();
        if (actual != null && !expiro(actual, now)) {
            return actual.payload;
        }
        PayloadParticipacion fresco = supplier.get();
        cache.set(new Entrada(fresco, now));
        return fresco;
    }

    private static boolean expiro(Entrada e, Instant now) {
        return Duration.between(e.creado, now).getSeconds() >= Constantes.TTL_PARTICIPACION_SEGUNDOS;
    }

    private record Entrada(PayloadParticipacion payload, Instant creado) {}
}
