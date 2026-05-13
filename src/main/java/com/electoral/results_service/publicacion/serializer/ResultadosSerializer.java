package com.electoral.results_service.publicacion.serializer;

import com.electoral.results_service.publicacion.AdvertenciaLegal;
import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.publicacion.dto.PayloadResultados;
import org.springframework.stereotype.Component;

/**
 * Construye exclusivamente {@link PayloadResultados}.
 *
 * <p>Inyecta la advertencia legal desde {@link AdvertenciaLegal} (constante en código
 * fuente). El número de día y el timestamp de cierre vienen del provider del
 * calendario — nunca son aceptados como parámetros desde la capa HTTP.
 *
 * <p>Reutiliza los campos de participación copiándolos a la nueva estructura: NO
 * comparte instancias con {@link ParticipacionSerializer} para evitar serialización
 * accidental del mismo objeto.
 */
@Component
public class ResultadosSerializer {

    public PayloadResultados serializar(PayloadParticipacion participacion, EstadoJornada estado) {
        return PayloadResultados.builder()
                .advertencia(AdvertenciaLegal.formatear(estado.getNumeroDia()))
                .numeroDiaJornada(estado.getNumeroDia())
                .timestampCierreDelDia(
                        estado.getTimestampCierreDelDia() != null
                                ? estado.getTimestampCierreDelDia().toString()
                                : null)
                .totalSufragantes(participacion.getTotalSufragantes())
                .porcentajeSobreCenso(participacion.getPorcentajeSobreCenso())
                .timestampActualizacion(participacion.getTimestampActualizacion())
                .fuente(participacion.getFuente())
                .build();
    }
}
