package com.electoral.results_service.publicacion;

import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.publicacion.dto.PayloadResultados;

/**
 * Motor de publicación electoral con segregación técnica de datos.
 *
 * <p>Garantías de la interfaz:
 * <ul>
 *   <li>El estado se determina exclusivamente desde el calendario (SR-M1). No hay
 *   forma de forzarlo por parámetro, header ni configuración.</li>
 *   <li>{@code publicarParticipacion()} siempre está disponible.</li>
 *   <li>{@code publicarResultados()} sólo es legal en {@link EstadoMotor#JORNADA_CERRADA_DIA};
 *   en caso contrario lanza {@link EstadoInvalidoException}.</li>
 * </ul>
 */
public interface MotorPublicacion {

    EstadoMotor obtenerEstadoActual();

    PayloadParticipacion publicarParticipacion();

    PayloadResultados publicarResultados();
}
