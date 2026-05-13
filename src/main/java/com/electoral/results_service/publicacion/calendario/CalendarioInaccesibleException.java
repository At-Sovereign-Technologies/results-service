package com.electoral.results_service.publicacion.calendario;

/** Lanzada cuando el puerto del calendario no puede entregar un estado confiable. */
public class CalendarioInaccesibleException extends RuntimeException {

    public CalendarioInaccesibleException(String message) {
        super(message);
    }

    public CalendarioInaccesibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
