package com.electoral.results_service.publicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.electoral.results_service.publicacion.calendario.EstadoJornada;
import com.electoral.results_service.publicacion.calendario.EstadoJornadaProvider;
import com.electoral.results_service.publicacion.dto.PayloadParticipacion;
import com.electoral.results_service.publicacion.dto.PayloadResultados;
import com.electoral.results_service.publicacion.serializer.ParticipacionSerializer;
import com.electoral.results_service.publicacion.serializer.ResultadosSerializer;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MotorPublicacionImpl — guardas de estado")
class MotorPublicacionImplTest {

    @Mock EstadoJornadaProvider provider;
    @Mock ParticipacionSerializer participacionSerializer;
    @Mock ResultadosSerializer resultadosSerializer;
    @InjectMocks MotorPublicacionImpl motor;

    @Test
    @DisplayName("publicarResultados() LANZA EstadoInvalidoException en JORNADA_ACTIVA")
    void resultados_rechazados_en_jornada_activa() {
        when(provider.obtener()).thenReturn(estado(EstadoMotor.JORNADA_ACTIVA, 1));

        assertThatThrownBy(() -> motor.publicarResultados())
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("solo se publican al cierre");
    }

    @Test
    @DisplayName("publicarResultados() permite la respuesta en JORNADA_CERRADA_DIA")
    void resultados_permitidos_en_jornada_cerrada() {
        when(provider.obtener()).thenReturn(estado(EstadoMotor.JORNADA_CERRADA_DIA, 2));
        PayloadParticipacion participacion = PayloadParticipacion.builder()
                .totalSufragantes(100)
                .porcentajeSobreCenso(1.0)
                .timestampActualizacion("t")
                .fuente("SISTEMA_CENTRAL_ELECTORAL")
                .build();
        when(participacionSerializer.serializar()).thenReturn(participacion);
        PayloadResultados esperado = PayloadResultados.builder()
                .advertencia("...")
                .numeroDiaJornada(2)
                .build();
        when(resultadosSerializer.serializar(participacion, estado(EstadoMotor.JORNADA_CERRADA_DIA, 2)))
                .thenReturn(esperado);

        PayloadResultados out = motor.publicarResultados();

        assertThat(out).isSameAs(esperado);
    }

    @Test
    @DisplayName("publicarParticipacion() funciona en ambos estados")
    void participacion_en_ambos_estados() {
        when(participacionSerializer.serializar())
                .thenReturn(PayloadParticipacion.builder().totalSufragantes(5L).build());

        assertThat(motor.publicarParticipacion().getTotalSufragantes()).isEqualTo(5);
        assertThat(motor.publicarParticipacion().getTotalSufragantes()).isEqualTo(5);
    }

    @Test
    @DisplayName("obtenerEstadoActual() devuelve el estado del provider")
    void delega_estado_al_provider() {
        when(provider.obtener()).thenReturn(estado(EstadoMotor.JORNADA_CERRADA_DIA, 1));
        assertThat(motor.obtenerEstadoActual()).isEqualTo(EstadoMotor.JORNADA_CERRADA_DIA);
    }

    private EstadoJornada estado(EstadoMotor e, int dia) {
        return EstadoJornada.builder()
                .estado(e)
                .numeroDia(dia)
                .timestampCierreDelDia(Instant.parse("2026-05-13T18:00:00Z"))
                .fechaInicioJornada(Instant.parse("2026-05-13T07:00:00Z"))
                .fechaFinJornada(Instant.parse("2026-05-15T18:00:00Z"))
                .timestampUltimaVerificacion(Instant.parse("2026-05-13T19:00:00Z"))
                .falloSeguro(false)
                .build();
    }
}
