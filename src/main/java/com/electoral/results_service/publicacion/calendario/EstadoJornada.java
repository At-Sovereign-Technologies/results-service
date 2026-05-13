package com.electoral.results_service.publicacion.calendario;

import com.electoral.results_service.publicacion.EstadoMotor;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * Value object inmutable que representa el estado del calendario en un instante.
 * Es el output del puerto {@link CalendarioElectoralPort} normalizado por el provider.
 */
@Value
@Builder
public class EstadoJornada {

    EstadoMotor estado;

    int numeroDia;

    Instant timestampCierreDelDia;

    Instant fechaInicioJornada;

    Instant fechaFinJornada;

    Instant timestampUltimaVerificacion;

    boolean falloSeguro;
}
