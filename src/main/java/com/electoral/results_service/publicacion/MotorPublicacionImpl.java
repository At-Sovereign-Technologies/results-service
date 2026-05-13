package com.electoral.results_service.publicacion;

import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.calendario.EstadoJornadaProvider;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.publicacion.dto.PayloadResultados;
import com.electoral.results_service.publicacion.serializer.ParticipacionSerializer;
import com.electoral.results_service.publicacion.serializer.ResultadosSerializer;
import org.springframework.stereotype.Service;

@Service
public class MotorPublicacionImpl implements MotorPublicacion {

    private final EstadoJornadaProvider provider;
    private final ParticipacionSerializer participacionSerializer;
    private final ResultadosSerializer resultadosSerializer;

    public MotorPublicacionImpl(
            EstadoJornadaProvider provider,
            ParticipacionSerializer participacionSerializer,
            ResultadosSerializer resultadosSerializer) {
        this.provider = provider;
        this.participacionSerializer = participacionSerializer;
        this.resultadosSerializer = resultadosSerializer;
    }

    @Override
    public EstadoMotor obtenerEstadoActual() {
        return provider.obtener().getEstado();
    }

    @Override
    public PayloadParticipacion publicarParticipacion() {
        return participacionSerializer.serializar();
    }

    @Override
    public PayloadResultados publicarResultados() {
        EstadoJornada estado = provider.obtener();
        if (estado.getEstado() != EstadoMotor.JORNADA_CERRADA_DIA) {
            throw new EstadoInvalidoException(
                    "Los resultados parciales solo se publican al cierre diario de la jornada.");
        }
        PayloadParticipacion participacion = participacionSerializer.serializar();
        return resultadosSerializer.serializar(participacion, estado);
    }

    public EstadoJornada estadoCompleto() {
        return provider.obtener();
    }
}
