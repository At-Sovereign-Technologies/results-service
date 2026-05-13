package com.electoral.results_service.publicacion.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import com.electoral.results_service.publicacion.AdvertenciaLegal;
import com.electoral.results_service.publicacion.EstadoMotor;
import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.publicacion.dto.PayloadResultados;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ResultadosSerializer — advertencia legal obligatoria")
class ResultadosSerializerTest {

    private final ResultadosSerializer serializer = new ResultadosSerializer();

    @Test
    @DisplayName("La advertencia aparece como campo de PRIMER NIVEL del JSON")
    void advertencia_primer_nivel() throws Exception {
        PayloadResultados payload = serializer.serializar(participacion(), estado(3));

        ObjectMapper om = new ObjectMapper();
        JsonNode json = om.valueToTree(payload);

        assertThat(json.has("advertencia")).isTrue();
        assertThat(json.get("advertencia").isTextual()).isTrue();
        assertThat(json.get("advertencia").asText()).contains("CIERRE DEL DÍA 3 —");
    }

    @Test
    @DisplayName("La advertencia coincide BYTE A BYTE con la plantilla esperada (con [N] sustituido)")
    void advertencia_exacta() {
        PayloadResultados payload = serializer.serializar(participacion(), estado(5));
        String esperada = AdvertenciaLegal.PLANTILLA.replace("[N]", "5");
        assertThat(payload.getAdvertencia()).isEqualTo(esperada);
    }

    @Test
    @DisplayName("Sustituye [N] correctamente para N=1, 7, 10")
    void sustituye_n_para_varios_valores() {
        assertThat(serializer.serializar(participacion(), estado(1)).getAdvertencia())
                .contains("CIERRE DEL DÍA 1 —")
                .doesNotContain("[N]");
        assertThat(serializer.serializar(participacion(), estado(7)).getAdvertencia())
                .contains("CIERRE DEL DÍA 7 —");
        assertThat(serializer.serializar(participacion(), estado(10)).getAdvertencia())
                .contains("CIERRE DEL DÍA 10 —");
    }

    @Test
    @DisplayName("Copia los campos de participación al payload de resultados")
    void copia_campos_de_participacion() {
        PayloadParticipacion p = participacion();
        PayloadResultados r = serializer.serializar(p, estado(1));
        assertThat(r.getTotalSufragantes()).isEqualTo(p.getTotalSufragantes());
        assertThat(r.getPorcentajeSobreCenso()).isEqualTo(p.getPorcentajeSobreCenso());
        assertThat(r.getFuente()).isEqualTo(p.getFuente());
    }

    private PayloadParticipacion participacion() {
        return PayloadParticipacion.builder()
                .totalSufragantes(5_000_000L)
                .porcentajeSobreCenso(50.00)
                .timestampActualizacion("2026-05-13T18:00:00Z")
                .fuente("SISTEMA_CENTRAL_ELECTORAL")
                .build();
    }

    private EstadoJornada estado(int dia) {
        return EstadoJornada.builder()
                .estado(EstadoMotor.JORNADA_CERRADA_DIA)
                .numeroDia(dia)
                .timestampCierreDelDia(Instant.parse("2026-05-13T18:00:00Z"))
                .fechaInicioJornada(Instant.parse("2026-05-13T07:00:00Z"))
                .fechaFinJornada(Instant.parse("2026-05-15T18:00:00Z"))
                .timestampUltimaVerificacion(Instant.parse("2026-05-13T19:00:00Z"))
                .falloSeguro(false)
                .build();
    }
}
