package com.electoral.results_service.publicacion;

/**
 * Estados mutuamente excluyentes del motor de publicación.
 *
 * <p>La transición entre estados es automática y depende exclusivamente del calendario
 * electoral (SR-M1). No existe ningún mecanismo — endpoint, flag, comando, rol — que
 * permita a un actor humano forzar la transición.
 */
public enum EstadoMotor {

    /** Jornada en curso: sólo se publica participación, nunca preferencias de voto. */
    JORNADA_ACTIVA,

    /** Día cerrado: se publican resultados parciales acumulados con advertencia legal. */
    JORNADA_CERRADA_DIA
}
