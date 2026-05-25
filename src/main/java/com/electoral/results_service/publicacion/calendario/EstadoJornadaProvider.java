package com.electoral.results_service.publicacion.calendario;

import com.electoral.results_service.publicacion.Constantes;
import com.electoral.results_service.publicacion.EstadoMotor;
import com.electoral.results_service.publicacion.calendario.AuditoriaTransicion.FuenteEstado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EstadoJornadaProvider {

    private static final Logger log = LoggerFactory.getLogger(EstadoJornadaProvider.class);

    private final CalendarioElectoralPort port;
    private final AuditoriaTransicion auditoria;
    private final Clock clock;

    private final AtomicReference<EstadoJornada> cache = new AtomicReference<>();
    private final AtomicReference<Instant> ultimaSincronizacionExitosa = new AtomicReference<>();

    @Autowired
    public EstadoJornadaProvider(CalendarioElectoralPort port, AuditoriaTransicion auditoria) {
        this(port, auditoria, Clock.systemUTC());
    }

    EstadoJornadaProvider(CalendarioElectoralPort port, AuditoriaTransicion auditoria, Clock clock) {
        this.port = port;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    public EstadoJornada obtener() {
        EstadoJornada cached = cache.get();
        Instant now = clock.instant();
        if (cached != null && !expiro(cached, now)) {
            return cached;
        }
        return refrescar(cached, now);
    }

    public Instant ultimaSincronizacionExitosa() {
        return ultimaSincronizacionExitosa.get();
    }

    private boolean expiro(EstadoJornada cached, Instant now) {
        Duration edad = Duration.between(cached.getTimestampUltimaVerificacion(), now);
        return edad.getSeconds() >= Constantes.TTL_ESTADO_MOTOR_SEGUNDOS;
    }

    private EstadoJornada refrescar(EstadoJornada anterior, Instant now) {
        EstadoJornada nuevo;
        try {
            Sr_M1Respuesta respuesta = port.consultar();
            nuevo = mapear(respuesta, now);
            ultimaSincronizacionExitosa.set(now);
            registrarTransicion(anterior, nuevo, FuenteEstado.SR_M1);
        } catch (RuntimeException ex) {
            log.warn("FALLO_SEGURO - SR-M1 inaccesible o datos corruptos: {}", ex.getMessage());
            nuevo = falloSeguro(now);
            registrarTransicion(anterior, nuevo, FuenteEstado.FALLO_SEGURO);
        }
        cache.set(nuevo);
        return nuevo;
    }

    private EstadoJornada mapear(Sr_M1Respuesta respuesta, Instant now) {
        if (respuesta == null || respuesta.getEstado() == null) {
            throw new CalendarioInaccesibleException("Respuesta SR-M1 incompleta");
        }
        Instant inicio = parsearInstante(respuesta.getFechaInicioJornada(), "fechaInicioJornada");
        Instant fin = parsearInstante(respuesta.getFechaFinJornada(), "fechaFinJornada");
        Instant cierreDia = respuesta.getTimestampCierreDia() != null
                ? parsearInstante(respuesta.getTimestampCierreDia(), "timestampCierreDia")
                : null;

        EstadoMotor estado = switch (respuesta.getEstado()) {
            case "ACTIVA" -> EstadoMotor.JORNADA_ACTIVA;
            case "CERRADA_DIA" -> EstadoMotor.JORNADA_CERRADA_DIA;
            default -> throw new CalendarioInaccesibleException(
                    "Estado SR-M1 desconocido: " + respuesta.getEstado());
        };

        int numeroDia = calcularNumeroDia(inicio, now);

        return EstadoJornada.builder()
                .estado(estado)
                .numeroDia(numeroDia)
                .timestampCierreDelDia(cierreDia)
                .fechaInicioJornada(inicio)
                .fechaFinJornada(fin)
                .timestampUltimaVerificacion(now)
                .falloSeguro(false)
                .build();
    }

    private static Instant parsearInstante(String iso, String campo) {
        if (iso == null) {
            throw new CalendarioInaccesibleException("Campo SR-M1 ausente: " + campo);
        }
        try {
            return Instant.parse(iso);
        } catch (RuntimeException ex) {
            throw new CalendarioInaccesibleException(
                    "Campo SR-M1 con formato inválido: " + campo + "=" + iso, ex);
        }
    }

    private static int calcularNumeroDia(Instant inicio, Instant now) {
        long segundos = Duration.between(inicio, now).getSeconds();
        if (segundos < 0) {
            return 1;
        }
        long dias = segundos / 86400L;
        return (int) (dias + 1);
    }

    private EstadoJornada falloSeguro(Instant now) {
        return EstadoJornada.builder()
                .estado(EstadoMotor.JORNADA_ACTIVA)
                .numeroDia(1)
                .timestampCierreDelDia(null)
                .fechaInicioJornada(null)
                .fechaFinJornada(null)
                .timestampUltimaVerificacion(now)
                .falloSeguro(true)
                .build();
    }

    private void registrarTransicion(EstadoJornada anterior, EstadoJornada nuevo, FuenteEstado fuente) {
        EstadoMotor estadoAnterior = anterior != null ? anterior.getEstado() : null;
        if (estadoAnterior != nuevo.getEstado()) {
            auditoria.registrar(estadoAnterior, nuevo.getEstado(), fuente);
        }
    }
}
