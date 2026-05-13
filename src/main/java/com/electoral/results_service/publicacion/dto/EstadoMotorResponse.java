package com.electoral.results_service.publicacion.dto;

import com.electoral.results_service.publicacion.EstadoMotor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload del endpoint interno {@code /api/v1/publicacion/estado}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoMotorResponse {

    private EstadoMotor estado;

    private String timestampUltimaVerificacionSrM1;

    private boolean falloSeguroActivo;
}
