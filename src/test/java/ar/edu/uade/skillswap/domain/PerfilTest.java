package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.RangoHorario;
import ar.edu.uade.skillswap.domain.model.TipoPerfil;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.soporte.Datos;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PerfilTest {

    @Test
    void arrancaActivoSinReputacionYConBandejaInterna() {
        Perfil perfil = Datos.profesional("u1");
        assertTrue(perfil.isActivo());
        assertEquals(0, perfil.getReputacion());
        assertEquals(0, perfil.getCantidadCalificaciones());
        assertEquals(Set.of(CanalNotificacion.INTERNA), perfil.getCanales());
        assertEquals(TipoPerfil.PROFESIONAL, perfil.getTipo());
        assertEquals(0, perfil.getVersion());
    }

    @Test
    void datosObligatorios() {
        assertThrows(ReglaNegocioException.class, () -> new Perfil(" ", TipoPerfil.PROFESIONAL, "N"));
        assertThrows(ReglaNegocioException.class, () -> new Perfil("u", null, "N"));
        assertThrows(ReglaNegocioException.class, () -> new Perfil("u", TipoPerfil.PROFESIONAL, ""));
    }

    @Test
    void administraSusDatos() {
        Perfil perfil = Datos.profesional("u1");
        Habilidad java = new Habilidad("Java");
        Habilidad ux = new Habilidad("UX");
        RangoHorario lunes = new RangoHorario(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        perfil.actualizarDatos("Ana", "Bio", ZoneId.of("UTC"));
        perfil.definirHabilidades(List.of(java), List.of(ux));
        perfil.definirDisponibilidad(List.of(lunes));
        perfil.elegirCanales(Set.of(CanalNotificacion.EMAIL, CanalNotificacion.PUSH));
        assertEquals("Ana", perfil.getNombreVisible());
        assertEquals("Bio", perfil.getBiografia());
        assertEquals(ZoneId.of("UTC"), perfil.getZonaHoraria());
        assertEquals(List.of(java), perfil.getHabilidades());
        assertEquals(List.of(ux), perfil.getIntereses());
        assertEquals(List.of(lunes), perfil.getDisponibilidad());
        assertEquals(Set.of(CanalNotificacion.EMAIL, CanalNotificacion.PUSH), perfil.getCanales());
        assertThrows(ReglaNegocioException.class, () -> perfil.elegirCanales(Set.of()));
    }

    @Test
    void soloLaOrganizacionPublicaDesafios() {
        assertTrue(Datos.organizacion("o").puedePublicar(TipoPublicacion.DESAFIO));
        assertFalse(Datos.organizacion("o").puedePublicar(TipoPublicacion.MENTORIA));
        assertTrue(Datos.profesional("p").puedePublicar(TipoPublicacion.SERVICIO));
        assertFalse(Datos.profesional("p").puedePublicar(TipoPublicacion.DESAFIO));
    }

    @Test
    void laBajaEsLogicaYNoPublica() {
        Perfil perfil = Datos.profesional("u1");
        perfil.darDeBaja();
        assertFalse(perfil.isActivo());
        assertFalse(perfil.puedePublicar(TipoPublicacion.MENTORIA));
        assertEquals("u1", perfil.getUserId());
    }
}
