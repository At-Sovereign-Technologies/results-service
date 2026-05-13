package com.electoral.results_service.publicacion.calendario;

import com.electoral.results_service.publicacion.EstadoMotor;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra cada transición de estado del motor con timestamp, estado anterior, estado
 * nuevo y fuente (SR-M1 o fallo seguro). Implementación basada en SLF4J con formato
 * estructurado de keys fijas para facilitar ingesta a sistemas de auditoría.
 */
@Component
public class AuditoriaTransicion {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaTransicion.class);

    public void registrar(EstadoMotor anterior, EstadoMotor nuevo, FuenteEstado fuente) {
        log.info(
                "AUDIT_MOTOR_TRANSICION timestamp={} estadoAnterior={} estadoNuevo={} fuente={}",
                Instant.now(),
                anterior,
                nuevo,
                fuente);
    }

    public enum FuenteEstado {
        SR_M1,
        FALLO_SEGURO
    }
}
