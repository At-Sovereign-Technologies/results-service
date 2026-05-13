package com.electoral.results_service.publicacion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload de participación electoral. Por construcción NO contiene ningún campo del
 * dominio de resultados (votos por candidato, advertencias, día de jornada, etc.).
 * Esa ausencia — no la nulidad ni el enmascaramiento — es la garantía de confidencialidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PayloadParticipacion {

    private long totalSufragantes;

    private double porcentajeSobreCenso;

    private String timestampActualizacion;

    private String fuente;
}
