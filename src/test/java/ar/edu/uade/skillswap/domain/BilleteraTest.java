package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.MovimientoCredito;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;
import ar.edu.uade.skillswap.soporte.Datos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BilleteraTest {

    private final Billetera billetera = new Billetera(Datos.profesional("u1"), new SkillCoins(1000));

    @Test
    void debitaYRegistraElMovimiento() {
        billetera.debitar(new SkillCoins(300), "Pago", 7L);
        assertEquals(new SkillCoins(700), billetera.getSaldo());
        MovimientoCredito m = billetera.getMovimientos().get(0);
        assertEquals(TipoMovimiento.DEBITO, m.getTipo());
        assertEquals(new SkillCoins(300), m.getMonto());
        assertEquals("Pago", m.getConcepto());
        assertEquals(7L, m.getContratacionId());
        assertNotNull(m.getFecha());
        assertTrue(billetera.yaPago(7L));
        assertFalse(billetera.yaPago(8L));
    }

    @Test
    void saldoInsuficienteNoModificaNada() {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> billetera.debitar(new SkillCoins(1001), "Pago", 1L));
        assertEquals("Saldo insuficiente", e.getMensaje());
        assertEquals(new SkillCoins(1000), billetera.getSaldo());
        assertTrue(billetera.getMovimientos().isEmpty());
    }

    @Test
    void noPermiteDobleCobroDeLaMismaContratacion() {
        billetera.debitar(new SkillCoins(100), "Pago", 1L);
        assertThrows(ReglaNegocioException.class, () -> billetera.debitar(new SkillCoins(100), "Pago", 1L));
        assertEquals(new SkillCoins(900), billetera.getSaldo());
    }

    @Test
    void acreditaConElTipoIndicado() {
        billetera.acreditar(new SkillCoins(50), TipoMovimiento.REINTEGRO, "Reintegro", 1L);
        assertEquals(new SkillCoins(1050), billetera.getSaldo());
        assertEquals(TipoMovimiento.REINTEGRO, billetera.getMovimientos().get(0).getTipo());
        assertTrue(billetera.tieneSaldo(new SkillCoins(1050)));
        assertEquals("u1", billetera.getTitular().getUserId());
    }

    @Test
    void necesitaTitularYSaldo() {
        assertThrows(ReglaNegocioException.class, () -> new Billetera(null, SkillCoins.CERO));
    }
}
