package com.electoral.results_service.publicacion;

/**
 * Constantes del motor de publicación. Estos TTLs NO se exponen como propiedades de
 * configuración runtime: la inmutabilidad de estos valores es un requisito de seguridad
 * arquitectónica. Cambiarlos requiere modificar el código y deja huella en git history.
 */
public final class Constantes {

    public static final int TTL_ESTADO_MOTOR_SEGUNDOS = 30;

    public static final int TTL_PARTICIPACION_SEGUNDOS = 5;

    public static final String FUENTE_OFICIAL = "SISTEMA_CENTRAL_ELECTORAL";

    private Constantes() {}
}
