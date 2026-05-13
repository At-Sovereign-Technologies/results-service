package com.electoral.results_service.publicacion;

/**
 * Lanzada cuando se solicita una operación incompatible con el estado actual del motor.
 * Caso canónico: pedir {@code publicarResultados()} mientras el motor está en
 * {@link EstadoMotor#JORNADA_ACTIVA}.
 */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String message) {
        super(message);
    }
}
