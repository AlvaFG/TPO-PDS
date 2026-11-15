package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.CambioEstado;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.PropuestaReprogramacion;
import ar.edu.uade.skillswap.domain.model.RangoHorario;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectsTest {

    private static final LocalDateTime LUNES_10 = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Test
    void rangoHorarioIncluyeSoloSuDiaYHorario() {
        RangoHorario rango = new RangoHorario(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        assertTrue(rango.incluye(LUNES_10));
        assertFalse(rango.incluye(LUNES_10.withHour(12)));
        assertFalse(rango.incluye(LUNES_10.plusDays(1)));
        assertEquals(DayOfWeek.MONDAY, rango.getDia());
        assertEquals(LocalTime.of(9, 0), rango.getDesde());
        assertEquals(LocalTime.of(12, 0), rango.getHasta());
        assertEquals(rango, new RangoHorario(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        assertEquals(rango.hashCode(), new RangoHorario(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)).hashCode());
    }

    @Test
    void rangoHorarioInvertidoLanza() {
        assertThrows(ReglaNegocioException.class,
                () -> new RangoHorario(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(9, 0)));
    }

    @Test
    void franjaSeOcupaUnaSolaVez() {
        FranjaHoraria franja = new FranjaHoraria(LUNES_10, LUNES_10.plusHours(1));
        assertTrue(franja.estaLibre());
        franja.ocupar();
        assertFalse(franja.estaLibre());
        assertThrows(ReglaNegocioException.class, franja::ocupar);
        franja.liberar();
        assertTrue(franja.estaLibre());
        assertEquals(LUNES_10, franja.getInicio());
        assertEquals(LUNES_10.plusHours(1), franja.getFin());
    }

    @Test
    void franjasSuperpuestas() {
        FranjaHoraria a = new FranjaHoraria(LUNES_10, LUNES_10.plusHours(1));
        assertTrue(a.seSuperponeCon(new FranjaHoraria(LUNES_10.plusMinutes(30), LUNES_10.plusHours(2))));
        assertFalse(a.seSuperponeCon(new FranjaHoraria(LUNES_10.plusHours(1), LUNES_10.plusHours(2))));
        assertThrows(ReglaNegocioException.class, () -> new FranjaHoraria(LUNES_10, LUNES_10));
    }

    @Test
    void propuestaLaAceptaSoloLaContraparte() {
        FranjaHoraria franja = new FranjaHoraria(LUNES_10, LUNES_10.plusHours(1));
        PropuestaReprogramacion propuesta = new PropuestaReprogramacion(franja, "cli", LUNES_10);
        assertFalse(propuesta.puedeAceptarla("cli"));
        assertTrue(propuesta.puedeAceptarla("prov"));
        assertSame(franja, propuesta.getNuevaFranja());
        assertEquals("cli", propuesta.getPropuestaPor());
        assertEquals(LUNES_10, propuesta.getFecha());
    }

    @Test
    void cambioEstadoGuardaFechaActorYMotivo() {
        CambioEstado cambio = new CambioEstado(LUNES_10, "u1", "BORRADOR", "PUBLICADA", "ok");
        assertEquals(LUNES_10, cambio.getFecha());
        assertEquals("u1", cambio.getActorId());
        assertEquals("BORRADOR", cambio.getEstadoAnterior());
        assertEquals("PUBLICADA", cambio.getEstadoNuevo());
        assertEquals("ok", cambio.getMotivo());
    }
}
