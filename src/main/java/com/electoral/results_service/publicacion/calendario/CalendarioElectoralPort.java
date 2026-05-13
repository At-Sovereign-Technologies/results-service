package com.electoral.results_service.publicacion.calendario;

/**
 * Puerto de salida hacia el calendario electoral (SR-M1). El motor de publicación es
 * un consumidor pasivo: no escribe ni modifica el calendario.
 *
 * <p>Las implementaciones DEBEN lanzar {@link CalendarioInaccesibleException} cuando no
 * puedan obtener el estado canónico (timeout, error de red, datos corruptos). El
 * provider que envuelve este puerto aplica la política de fallo seguro.
 */
public interface CalendarioElectoralPort {

    Sr_M1Respuesta consultar() throws CalendarioInaccesibleException;
}
