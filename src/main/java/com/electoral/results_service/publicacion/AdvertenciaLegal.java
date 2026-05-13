package com.electoral.results_service.publicacion;

/**
 * Advertencia legal obligatoria adjunta a toda respuesta de resultados parciales.
 * Definida como constante en código fuente — no en BD, no en configuración — para que
 * cualquier modificación sea trazable en el historial de commits.
 */
public final class AdvertenciaLegal {

    public static final String PLANTILLA =
            "RESULTADOS PARCIALES ACUMULADOS AL CIERRE DEL DÍA [N] — "
            + "La jornada electoral continúa. "
            + "Estos datos pueden influir en su decisión de voto. "
            + "Los resultados oficiales dependen del escrutinio físico y la declaratoria del CNE.";

    private AdvertenciaLegal() {}

    public static String formatear(int numeroDiaJornada) {
        if (numeroDiaJornada < 1) {
            throw new IllegalArgumentException("numeroDiaJornada debe ser >= 1");
        }
        return PLANTILLA.replace("[N]", Integer.toString(numeroDiaJornada));
    }
}
