package com.electoral.results_service.publicacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload de resultados parciales acumulados. Intencionalmente NO hereda de
 * {@link PayloadParticipacion}: una clase separada garantiza que no se comparta el mismo
 * objeto entre los dos modos y que la advertencia legal sea siempre serializada como
 * campo de primer nivel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayloadResultados {

    private String advertencia;

    private int numeroDiaJornada;

    private String timestampCierreDelDia;

    private long totalSufragantes;

    private double porcentajeSobreCenso;

    private String timestampActualizacion;

    private String fuente;
}
