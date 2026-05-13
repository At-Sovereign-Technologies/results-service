package com.electoral.results_service.controller;

import com.electoral.results_service.publicacion.MotorPublicacion;
import com.electoral.results_service.publicacion.cache.ParticipacionCache;
import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.calendario.EstadoJornadaProvider;
import com.electoral.results_service.publicacion.dto.EstadoMotorResponse;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.publicacion.dto.PayloadResultados;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/publicacion")
public class PublicacionController {

    private final MotorPublicacion motor;
    private final EstadoJornadaProvider provider;
    private final ParticipacionCache participacionCache;

    public PublicacionController(
            MotorPublicacion motor,
            EstadoJornadaProvider provider,
            ParticipacionCache participacionCache) {
        this.motor = motor;
        this.provider = provider;
        this.participacionCache = participacionCache;
    }

    @GetMapping("/participacion")
    @Operation(summary = "Publica datos de participación (siempre disponible durante la jornada)")
    public PayloadParticipacion participacion() {
        return participacionCache.obtener(motor::publicarParticipacion);
    }

    @GetMapping("/resultados")
    @Operation(summary = "Publica resultados parciales (sólo en JORNADA_CERRADA_DIA)")
    public PayloadResultados resultados() {
        return motor.publicarResultados();
    }

    @GetMapping("/estado")
    @Operation(summary = "Estado actual del motor (endpoint interno de monitoreo)")
    public EstadoMotorResponse estado() {
        EstadoJornada estadoJornada = provider.obtener();
        return EstadoMotorResponse.builder()
                .estado(estadoJornada.getEstado())
                .timestampUltimaVerificacionSrM1(
                        estadoJornada.getTimestampUltimaVerificacion().toString())
                .falloSeguroActivo(estadoJornada.isFalloSeguro())
                .build();
    }
}
