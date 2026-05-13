package com.electoral.results_service.publicacion.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.electoral.results_service.entity.Result;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.repository.ResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipacionSerializer — confidencialidad")
class ParticipacionSerializerTest {

    @Mock ResultRepository repository;

    ParticipacionSerializer serializer;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneOffset.UTC);
        serializer = new ParticipacionSerializer(repository, 10_000_000L, fixed);
    }

    @Test
    @DisplayName("Calcula totalSufragantes y porcentaje sobre censo correctamente")
    void calcula_sufragantes_y_porcentaje() {
        when(repository.findAll())
                .thenReturn(List.of(
                        new Result(1L, 1L, "A", 1_000_000),
                        new Result(2L, 1L, "B", 500_000),
                        new Result(3L, 2L, "C", 1_500_000)));

        PayloadParticipacion payload = serializer.serializar();

        assertThat(payload.getTotalSufragantes()).isEqualTo(3_000_000L);
        assertThat(payload.getPorcentajeSobreCenso()).isEqualTo(30.00);
        assertThat(payload.getFuente()).isEqualTo("SISTEMA_CENTRAL_ELECTORAL");
        assertThat(payload.getTimestampActualizacion()).isEqualTo("2026-05-13T10:00:00Z");
    }

    @Test
    @DisplayName("INVARIANTE: el JSON serializado NO contiene ninguna clave del dominio de resultados")
    void no_filtra_campos_de_resultados() throws Exception {
        when(repository.findAll()).thenReturn(List.of(new Result(1L, 1L, "A", 100)));
        PayloadParticipacion payload = serializer.serializar();

        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.valueToTree(payload);

        List<String> camposProhibidos = List.of(
                "advertencia",
                "numeroDiaJornada",
                "timestampCierreDelDia",
                "resultadosParciales",
                "porcentajeActasEscrutadas",
                "candidatoId",
                "candidateName",
                "partido",
                "votos",
                "votes");

        for (String campo : camposProhibidos) {
            assertThat(json.has(campo))
                    .as("PayloadParticipacion no debe contener la clave '%s' (ni vacía ni nula)", campo)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Maneja repositorio vacío sin error")
    void repositorio_vacio() {
        when(repository.findAll()).thenReturn(List.of());
        PayloadParticipacion payload = serializer.serializar();
        assertThat(payload.getTotalSufragantes()).isZero();
        assertThat(payload.getPorcentajeSobreCenso()).isZero();
    }
}
