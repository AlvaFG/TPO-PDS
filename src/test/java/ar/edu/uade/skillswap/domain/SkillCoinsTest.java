package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillCoinsTest {

    @Test
    void noAdmiteMontosNegativos() {
        assertThrows(ReglaNegocioException.class, () -> new SkillCoins(-1));
    }

    @Test
    void sumaYRestaDevuelvenInstanciasNuevas() {
        SkillCoins cien = new SkillCoins(100);
        assertEquals(new SkillCoins(150), cien.sumar(new SkillCoins(50)));
        assertEquals(new SkillCoins(70), cien.restar(new SkillCoins(30)));
        assertEquals(100, cien.getCantidad());
    }

    @Test
    void restarMasDeLoQueHayLanza() {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> new SkillCoins(10).restar(new SkillCoins(11)));
        assertTrue(e.getMensaje().contains("Saldo insuficiente"));
    }

    @Test
    void restarHastaCeroNuncaDaNegativo() {
        assertEquals(SkillCoins.CERO, new SkillCoins(10).restarHastaCero(new SkillCoins(500)));
        assertEquals(new SkillCoins(4), new SkillCoins(10).restarHastaCero(new SkillCoins(6)));
    }

    @Test
    void porcentajeYComparacion() {
        assertEquals(new SkillCoins(50), new SkillCoins(100).porcentaje(50));
        assertThrows(ReglaNegocioException.class, () -> new SkillCoins(100).porcentaje(101));
        assertThrows(ReglaNegocioException.class, () -> new SkillCoins(100).porcentaje(-1));
        assertTrue(new SkillCoins(1).esMenorQue(new SkillCoins(2)));
        assertFalse(new SkillCoins(2).esMenorQue(new SkillCoins(2)));
    }

    @Test
    void igualdadPorValor() {
        assertEquals(new SkillCoins(5).hashCode(), new SkillCoins(5).hashCode());
        assertNotEquals(new SkillCoins(5), "5");
        assertEquals("5 SC", new SkillCoins(5).toString());
    }
}
