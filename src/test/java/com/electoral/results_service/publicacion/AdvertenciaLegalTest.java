package com.electoral.results_service.publicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdvertenciaLegal — constante e inmutable")
class AdvertenciaLegalTest {

    @Test
    @DisplayName("La plantilla coincide exactamente con la cadena requerida por la tarea")
    void plantilla_exacta() {
        String esperada =
                "RESULTADOS PARCIALES ACUMULADOS AL CIERRE DEL DÍA [N] — "
                        + "La jornada electoral continúa. "
                        + "Estos datos pueden influir en su decisión de voto. "
                        + "Los resultados oficiales dependen del escrutinio físico y la declaratoria del CNE.";
        assertThat(AdvertenciaLegal.PLANTILLA).isEqualTo(esperada);
    }

    @Test
    @DisplayName("formatear sustituye [N] por el día indicado (N=1, 7, 10)")
    void formatear_sustituye_n() {
        assertThat(AdvertenciaLegal.formatear(1)).contains("CIERRE DEL DÍA 1 —");
        assertThat(AdvertenciaLegal.formatear(7)).contains("CIERRE DEL DÍA 7 —");
        assertThat(AdvertenciaLegal.formatear(10)).contains("CIERRE DEL DÍA 10 —");
        assertThat(AdvertenciaLegal.formatear(1)).doesNotContain("[N]");
    }

    @Test
    @DisplayName("formatear rechaza N < 1")
    void formatear_rechaza_n_invalido() {
        assertThatThrownBy(() -> AdvertenciaLegal.formatear(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AdvertenciaLegal.formatear(-3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
