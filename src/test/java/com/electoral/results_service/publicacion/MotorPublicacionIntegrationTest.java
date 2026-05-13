package com.electoral.results_service.publicacion;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.electoral.results_service.controller.PublicacionController;
import com.electoral.results_service.entity.Result;
import com.electoral.results_service.exception.GlobalExceptionHandler;
import com.electoral.results_service.publicacion.cache.ParticipacionCache;
import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.calendario.EstadoJornadaProvider;
import com.electoral.results_service.publicacion.serializer.ParticipacionSerializer;
import com.electoral.results_service.publicacion.serializer.ResultadosSerializer;
import com.electoral.results_service.repository.ResultRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test de flujo end-to-end del motor de publicación contra el controller REST.
 *
 * <p>Usa los beans REALES del motor: serializadores, motor implementation, cache de
 * participación. Sólo mockea las dependencias externas: {@link EstadoJornadaProvider}
 * (canal hacia SR-M1) y {@link ResultRepository} (datos de dominio).
 *
 * <p>Esto evita {@code @SpringBootTest} completo (que requeriría Redis + DB) y a la
 * vez ejercita la integración real entre controller → motor → serializers.
 */
@WebMvcTest(PublicacionController.class)
@Import({
    GlobalExceptionHandler.class,
    MotorPublicacionImpl.class,
    ParticipacionSerializer.class,
    ResultadosSerializer.class,
    ParticipacionCache.class
})
@DisplayName("Motor de publicación — flujo de integración (jornada activa → cierre → apertura)")
class MotorPublicacionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockBean EstadoJornadaProvider provider;
    @MockBean ResultRepository resultRepository;

    @Test
    @DisplayName("activa → participación 200 → resultados 403 → cierre → resultados 200 con advertencia → apertura día 2 → resultados 403")
    void flujo_completo() throws Exception {
        when(resultRepository.findAll()).thenReturn(List.of(new Result(1L, 1L, "X", 1_000_000)));

        // FASE 1 — DÍA 1, JORNADA_ACTIVA
        when(provider.obtener()).thenReturn(estadoActivo(1));

        mockMvc.perform(get("/api/v1/publicacion/participacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSufragantes").value(1_000_000))
                .andExpect(jsonPath("$.advertencia").doesNotExist())
                .andExpect(jsonPath("$.numeroDiaJornada").doesNotExist());

        mockMvc.perform(get("/api/v1/publicacion/resultados"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("RESULTADOS_NO_DISPONIBLES_EN_JORNADA_ACTIVA"));

        // FASE 2 — CIERRE DEL DÍA 1, JORNADA_CERRADA_DIA
        when(provider.obtener()).thenReturn(estadoCerrado(1));

        mockMvc.perform(get("/api/v1/publicacion/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertencia").exists())
                .andExpect(
                        jsonPath("$.advertencia")
                                .value(org.hamcrest.Matchers.containsString("CIERRE DEL DÍA 1 —")))
                .andExpect(jsonPath("$.numeroDiaJornada").value(1));

        // FASE 3 — APERTURA DÍA 2, JORNADA_ACTIVA otra vez
        when(provider.obtener()).thenReturn(estadoActivo(2));

        mockMvc.perform(get("/api/v1/publicacion/resultados"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("RESULTADOS_NO_DISPONIBLES_EN_JORNADA_ACTIVA"));

        // FASE 4 — CIERRE DEL DÍA 2, ahora con N=2 en la advertencia
        when(provider.obtener()).thenReturn(estadoCerrado(2));

        mockMvc.perform(get("/api/v1/publicacion/resultados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDiaJornada").value(2))
                .andExpect(
                        jsonPath("$.advertencia")
                                .value(org.hamcrest.Matchers.containsString("CIERRE DEL DÍA 2 —")));
    }

    private EstadoJornada estadoActivo(int dia) {
        return EstadoJornada.builder()
                .estado(EstadoMotor.JORNADA_ACTIVA)
                .numeroDia(dia)
                .fechaInicioJornada(Instant.parse("2026-05-13T07:00:00Z"))
                .fechaFinJornada(Instant.parse("2026-05-15T18:00:00Z"))
                .timestampUltimaVerificacion(Instant.now(Clock.systemUTC().withZone(ZoneOffset.UTC)))
                .falloSeguro(false)
                .build();
    }

    private EstadoJornada estadoCerrado(int dia) {
        return EstadoJornada.builder()
                .estado(EstadoMotor.JORNADA_CERRADA_DIA)
                .numeroDia(dia)
                .timestampCierreDelDia(Instant.parse("2026-05-13T18:00:00Z").plusSeconds(86400L * (dia - 1)))
                .fechaInicioJornada(Instant.parse("2026-05-13T07:00:00Z"))
                .fechaFinJornada(Instant.parse("2026-05-15T18:00:00Z"))
                .timestampUltimaVerificacion(Instant.now(Clock.systemUTC().withZone(ZoneOffset.UTC)))
                .falloSeguro(false)
                .build();
    }
}
