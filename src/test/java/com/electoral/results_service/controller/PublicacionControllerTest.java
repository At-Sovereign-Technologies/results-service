package com.electoral.results_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.electoral.results_service.exception.GlobalExceptionHandler;
import com.electoral.results_service.publicacion.EstadoInvalidoException;
import com.electoral.results_service.publicacion.EstadoMotor;
import com.electoral.results_service.publicacion.MotorPublicacion;
import com.electoral.results_service.publicacion.cache.ParticipacionCache;
import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.calendario.EstadoJornadaProvider;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.publicacion.dto.PayloadResultados;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicacionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("PublicacionController — MockMvc")
class PublicacionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MotorPublicacion motor;
    @MockBean EstadoJornadaProvider provider;
    @MockBean ParticipacionCache participacionCache;

    @Test
    @DisplayName("GET /participacion → 200 con payload sin claves de resultados")
    void get_participacion_ok() throws Exception {
        PayloadParticipacion p = PayloadParticipacion.builder()
                .totalSufragantes(1_000_000L)
                .porcentajeSobreCenso(10.5)
                .timestampActualizacion("2026-05-13T10:00:00Z")
                .fuente("SISTEMA_CENTRAL_ELECTORAL")
                .build();
        when(participacionCache.obtener(any())).thenAnswer(inv -> {
            Supplier<PayloadParticipacion> s = inv.getArgument(0);
            return s.get();
        });
        when(motor.publicarParticipacion()).thenReturn(p);

        mockMvc.perform(get("/api/v1/publicacion/participacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSufragantes").value(1_000_000))
                .andExpect(jsonPath("$.porcentajeSobreCenso").value(10.5))
                .andExpect(jsonPath("$.fuente").value("SISTEMA_CENTRAL_ELECTORAL"))
                .andExpect(jsonPath("$.advertencia").doesNotExist())
                .andExpect(jsonPath("$.numeroDiaJornada").doesNotExist())
                .andExpect(jsonPath("$.resultadosParciales").doesNotExist());
    }

    @Test
    @DisplayName("GET /resultados en JORNADA_ACTIVA → 403 con error tipificado")
    void get_resultados_bloqueado_en_jornada_activa() throws Exception {
        when(motor.publicarResultados())
                .thenThrow(new EstadoInvalidoException(
                        "Los resultados parciales solo se publican al cierre diario de la jornada."));

        mockMvc.perform(get("/api/v1/publicacion/resultados"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("RESULTADOS_NO_DISPONIBLES_EN_JORNADA_ACTIVA"))
                .andExpect(
                        jsonPath("$.mensaje")
                                .value(
                                        "Los resultados parciales solo se publican al cierre diario de la jornada."));
    }

    @Test
    @DisplayName("GET /resultados en JORNADA_CERRADA_DIA → 200 con advertencia exacta")
    void get_resultados_ok_con_advertencia() throws Exception {
        PayloadResultados r = PayloadResultados.builder()
                .advertencia(
                        "RESULTADOS PARCIALES ACUMULADOS AL CIERRE DEL DÍA 2 — "
                                + "La jornada electoral continúa. "
                                + "Estos datos pueden influir en su decisión de voto. "
                                + "Los resultados oficiales dependen del escrutinio físico y la declaratoria del CNE.")
                .numeroDiaJornada(2)
                .timestampCierreDelDia("2026-05-14T18:00:00Z")
                .totalSufragantes(2_000_000L)
                .porcentajeSobreCenso(20.0)
                .timestampActualizacion("2026-05-14T18:00:00Z")
                .fuente("SISTEMA_CENTRAL_ELECTORAL")
                .build();
        when(motor.publicarResultados()).thenReturn(r);

        mockMvc.perform(get("/api/v1/publicacion/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertencia").exists())
                .andExpect(jsonPath("$.advertencia").value(r.getAdvertencia()))
                .andExpect(jsonPath("$.numeroDiaJornada").value(2));
    }

    @Test
    @DisplayName("GET /estado → 200 con estado y timestamp")
    void get_estado_ok() throws Exception {
        EstadoJornada estado = EstadoJornada.builder()
                .estado(EstadoMotor.JORNADA_ACTIVA)
                .numeroDia(1)
                .fechaInicioJornada(Instant.parse("2026-05-13T07:00:00Z"))
                .fechaFinJornada(Instant.parse("2026-05-15T18:00:00Z"))
                .timestampUltimaVerificacion(Instant.parse("2026-05-13T10:00:00Z"))
                .falloSeguro(false)
                .build();
        when(provider.obtener()).thenReturn(estado);

        mockMvc.perform(get("/api/v1/publicacion/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("JORNADA_ACTIVA"))
                .andExpect(jsonPath("$.timestampUltimaVerificacionSrM1").value("2026-05-13T10:00:00Z"))
                .andExpect(jsonPath("$.falloSeguroActivo").value(false));
    }
}
