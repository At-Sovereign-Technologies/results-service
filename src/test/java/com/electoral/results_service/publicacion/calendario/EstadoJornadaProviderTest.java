package com.electoral.results_service.publicacion.calendario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.electoral.results_service.publicacion.EstadoMotor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstadoJornadaProvider — caché y política de fallo seguro")
class EstadoJornadaProviderTest {

    @Mock CalendarioElectoralPort port;
    @Mock AuditoriaTransicion auditoria;

    @Test
    @DisplayName("Caché TTL: dos llamadas en <30s consultan SR-M1 una sola vez")
    void cache_ttl_evita_doble_consulta() {
        Clock fixed = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneOffset.UTC);
        EstadoJornadaProvider provider = new EstadoJornadaProvider(port, auditoria, fixed);
        when(port.consultar()).thenReturn(respuestaActiva());

        provider.obtener();
        provider.obtener();

        verify(port, times(1)).consultar();
    }

    @Test
    @DisplayName("FALLO SEGURO: si el port lanza, retorna JORNADA_ACTIVA")
    void fallo_seguro_cuando_port_falla() {
        Clock fixed = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneOffset.UTC);
        EstadoJornadaProvider provider = new EstadoJornadaProvider(port, auditoria, fixed);
        when(port.consultar())
                .thenThrow(new CalendarioInaccesibleException("timeout simulado"));

        EstadoJornada estado = provider.obtener();

        assertThat(estado.getEstado()).isEqualTo(EstadoMotor.JORNADA_ACTIVA);
        assertThat(estado.isFalloSeguro()).isTrue();
    }

    @Test
    @DisplayName("FALLO SEGURO: respuesta con estado desconocido retorna JORNADA_ACTIVA")
    void fallo_seguro_cuando_datos_corruptos() {
        Clock fixed = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneOffset.UTC);
        EstadoJornadaProvider provider = new EstadoJornadaProvider(port, auditoria, fixed);
        Sr_M1Respuesta corrupta = new Sr_M1Respuesta();
        corrupta.setEstado("ZOMBIE");
        corrupta.setFechaInicioJornada("2026-05-13T07:00:00Z");
        corrupta.setFechaFinJornada("2026-05-15T18:00:00Z");
        when(port.consultar()).thenReturn(corrupta);

        EstadoJornada estado = provider.obtener();

        assertThat(estado.getEstado()).isEqualTo(EstadoMotor.JORNADA_ACTIVA);
        assertThat(estado.isFalloSeguro()).isTrue();
    }

    @Test
    @DisplayName("FALLO SEGURO: fecha de inicio con formato inválido")
    void fallo_seguro_cuando_fechas_invalidas() {
        Clock fixed = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneOffset.UTC);
        EstadoJornadaProvider provider = new EstadoJornadaProvider(port, auditoria, fixed);
        Sr_M1Respuesta r = new Sr_M1Respuesta();
        r.setEstado("ACTIVA");
        r.setFechaInicioJornada("no-es-iso");
        r.setFechaFinJornada("2026-05-15T18:00:00Z");
        when(port.consultar()).thenReturn(r);

        assertThat(provider.obtener().isFalloSeguro()).isTrue();
    }

    @Test
    @DisplayName("Calcula numeroDia correctamente desde fechaInicioJornada")
    void calcula_numero_dia() {
        Instant inicio = Instant.parse("2026-05-13T07:00:00Z");
        Clock dia1 = Clock.fixed(inicio.plusSeconds(3600), ZoneOffset.UTC);
        Clock dia2 = Clock.fixed(inicio.plusSeconds(86400 + 3600), ZoneOffset.UTC);
        Clock dia3 = Clock.fixed(inicio.plusSeconds(86400 * 2 + 3600), ZoneOffset.UTC);

        when(port.consultar()).thenReturn(respuestaActiva());

        EstadoJornadaProvider p1 = new EstadoJornadaProvider(port, auditoria, dia1);
        EstadoJornadaProvider p2 = new EstadoJornadaProvider(port, auditoria, dia2);
        EstadoJornadaProvider p3 = new EstadoJornadaProvider(port, auditoria, dia3);

        assertThat(p1.obtener().getNumeroDia()).isEqualTo(1);
        assertThat(p2.obtener().getNumeroDia()).isEqualTo(2);
        assertThat(p3.obtener().getNumeroDia()).isEqualTo(3);
    }

    @Test
    @DisplayName("Auditoría: registra transición cuando cambia el estado")
    void audita_transicion() {
        Clock fixed = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneOffset.UTC);
        EstadoJornadaProvider provider = new EstadoJornadaProvider(port, auditoria, fixed);

        when(port.consultar()).thenThrow(new CalendarioInaccesibleException("x"));
        provider.obtener();

        verify(auditoria, times(1))
                .registrar(
                        null,
                        EstadoMotor.JORNADA_ACTIVA,
                        AuditoriaTransicion.FuenteEstado.FALLO_SEGURO);
    }

    @Test
    @DisplayName("Auditoría: NO registra si el estado se mantiene entre llamadas")
    void no_audita_si_no_cambia() {
        Instant base = Instant.parse("2026-05-13T10:00:00Z");
        Clock fixed = Clock.fixed(base, ZoneOffset.UTC);
        EstadoJornadaProvider provider = new EstadoJornadaProvider(port, auditoria, fixed);
        when(port.consultar()).thenReturn(respuestaActiva());

        provider.obtener();
        provider.obtener();

        verify(auditoria, times(1))
                .registrar(null, EstadoMotor.JORNADA_ACTIVA, AuditoriaTransicion.FuenteEstado.SR_M1);
        verify(auditoria, never())
                .registrar(
                        EstadoMotor.JORNADA_ACTIVA,
                        EstadoMotor.JORNADA_ACTIVA,
                        AuditoriaTransicion.FuenteEstado.SR_M1);
    }

    private Sr_M1Respuesta respuestaActiva() {
        Sr_M1Respuesta r = new Sr_M1Respuesta();
        r.setEstado("ACTIVA");
        r.setFechaInicioJornada("2026-05-13T07:00:00Z");
        r.setFechaFinJornada("2026-05-15T18:00:00Z");
        return r;
    }
}
