package com.electoral.results_service.publicacion.calendario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de transporte del contrato HTTP idealizado de SR-M1.
 *
 * <p>Contrato esperado de {@code GET {srm1.base-url}/api/v1/calendario/estado-jornada}:
 * <pre>
 * {
 *   "estado": "ACTIVA" | "CERRADA_DIA",
 *   "fechaInicioJornada": "2026-05-13T07:00:00Z",
 *   "fechaFinJornada":    "2026-05-15T18:00:00Z",
 *   "timestampCierreDia": "2026-05-13T18:00:00Z"   // sólo si estado == CERRADA_DIA
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sr_M1Respuesta {

    private String estado;
    private String fechaInicioJornada;
    private String fechaFinJornada;
    private String timestampCierreDia;
}
