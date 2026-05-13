package com.electoral.results_service.publicacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estructura de error específica para el endpoint de resultados cuando el motor está en
 * estado JORNADA_ACTIVA. Mantiene el formato literal exigido por el contrato.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorPublicacionResponse {

    private String error;

    private String mensaje;
}
