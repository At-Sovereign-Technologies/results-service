package com.electoral.results_service.publicacion.serializer;

import com.electoral.results_service.entity.Result;
import com.electoral.results_service.publicacion.Constantes;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.repository.ResultRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Construye exclusivamente {@link PayloadParticipacion}.
 *
 * <p><b>Invariante de confidencialidad:</b> esta clase NO conoce, importa ni produce
 * ningún campo del dominio de resultados (preferencias por candidato, advertencia,
 * número de día). Cualquier salida que requiera esos datos debe ir por
 * {@link ResultadosSerializer}.
 */
@Component
public class ParticipacionSerializer {

    private final ResultRepository resultRepository;
    private final long censoElectoral;
    private final Clock clock;

    @Autowired
    public ParticipacionSerializer(
            ResultRepository resultRepository,
            @Value("${publicacion.censo-electoral:38000000}") long censoElectoral) {
        this(resultRepository, censoElectoral, Clock.systemUTC());
    }

    ParticipacionSerializer(ResultRepository resultRepository, long censoElectoral, Clock clock) {
        this.resultRepository = resultRepository;
        this.censoElectoral = censoElectoral;
        this.clock = clock;
    }

    public PayloadParticipacion serializar() {
        long totalSufragantes = totalSufragantes();
        double porcentaje = porcentajeSobreCenso(totalSufragantes);
        return PayloadParticipacion.builder()
                .totalSufragantes(totalSufragantes)
                .porcentajeSobreCenso(porcentaje)
                .timestampActualizacion(Instant.now(clock).toString())
                .fuente(Constantes.FUENTE_OFICIAL)
                .build();
    }

    private long totalSufragantes() {
        List<Result> todos = resultRepository.findAll();
        return todos.stream()
                .mapToLong(r -> r.getVotes() == null ? 0L : r.getVotes())
                .sum();
    }

    private double porcentajeSobreCenso(long totalSufragantes) {
        if (censoElectoral <= 0) {
            return 0.0;
        }
        BigDecimal pct = BigDecimal.valueOf(totalSufragantes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(censoElectoral), 2, RoundingMode.HALF_UP);
        return pct.doubleValue();
    }
}
