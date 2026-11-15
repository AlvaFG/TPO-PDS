package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;
import ar.edu.uade.skillswap.domain.politica.PoliticaEstricta;
import ar.edu.uade.skillswap.domain.politica.PoliticaFlexible;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/* Strategy: el mismo pago y la misma anticipacion dan reintegros distintos segun la politica. */
class PoliticaCancelacionTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 1, 10, 0);
    private static final SkillCoins PAGADO = new SkillCoins(1000);

    @ParameterizedTest(name = "flexible con {0} h de anticipacion reintegra {1}")
    @CsvSource({"48, 1000", "24, 1000", "23, 500", "2, 500", "1, 0", "-1, 0"})
    void flexible(long horas, long esperado) {
        assertEquals(new SkillCoins(esperado),
                new PoliticaFlexible().calcularReintegro(PAGADO, INICIO, INICIO.minusHours(horas)));
    }

    @ParameterizedTest(name = "estricta con {0} h de anticipacion reintegra {1}")
    @CsvSource({"100, 500", "72, 500", "71, 0", "1, 0"})
    void estricta(long horas, long esperado) {
        assertEquals(new SkillCoins(esperado),
                new PoliticaEstricta().calcularReintegro(PAGADO, INICIO, INICIO.minusHours(horas)));
    }

    @Test
    void losUmbralesSonConfigurables() {
        PoliticaCancelacion flexible = new PoliticaFlexible(10, 5);
        assertEquals(PAGADO, flexible.calcularReintegro(PAGADO, INICIO, INICIO.minusHours(10)));
        assertEquals(new SkillCoins(500), flexible.calcularReintegro(PAGADO, INICIO, INICIO.minusHours(5)));
        PoliticaCancelacion estricta = new PoliticaEstricta(1);
        assertEquals(new SkillCoins(500), estricta.calcularReintegro(PAGADO, INICIO, INICIO.minusHours(1)));
        assertEquals("Flexible", flexible.getNombre());
        assertEquals("Estricta", estricta.getNombre());
        assertNull(flexible.getId());
    }

    @Test
    void umbralesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new PoliticaFlexible(1, 5));
        assertThrows(IllegalArgumentException.class, () -> new PoliticaFlexible(5, -1));
        assertThrows(IllegalArgumentException.class, () -> new PoliticaEstricta(-1));
    }
}
