package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.dto.PerfilCommand;
import ar.edu.uade.skillswap.application.dto.PerfilPublicoDto;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.service.PerfilService;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.RangoHorario;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoPerfil;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

class PerfilServiceTest {

    private final Repositorios.Perfiles perfiles = new Repositorios.Perfiles();
    private final Repositorios.Publicaciones publicaciones = new Repositorios.Publicaciones();
    private final Repositorios.Contrataciones contrataciones = new Repositorios.Contrataciones();
    private final Repositorios.Catalogo catalogo = new Repositorios.Catalogo();
    private final Repositorios.Billeteras billeteras = new Repositorios.Billeteras();
    private PerfilService service;
    private Habilidad java;

    @BeforeEach
    void preparar() {
        service = new PerfilService(perfiles, publicaciones, contrataciones, catalogo, billeteras,
                new Repositorios.GestorTransacciones());
        java = catalogo.guardar(new Habilidad("Java"));
    }

    private PerfilCommand comando(String nombre, String zona) {
        return new PerfilCommand(TipoPerfil.PROFESIONAL, nombre, "Backend", zona, List.of(java.getId()), List.of(),
                List.of(new RangoHorario(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))),
                Set.of(CanalNotificacion.EMAIL));
    }

    @Test
    void crearPerfilConBilleteraYReputacionCero() {
        PerfilPublicoDto dto = service.crearPerfil("u1", comando("Ana", "America/Argentina/Buenos_Aires"));
        assertEquals("Ana", dto.nombreVisible());
        assertEquals("Backend", dto.biografia());
        assertEquals(0, dto.reputacion());
        assertEquals(0, dto.cantidadCalificaciones());
        assertEquals(List.of("Java"), dto.habilidades());
        assertEquals(SkillCoins.CERO, billeteras.buscarPorUserId("u1").orElseThrow().getSaldo());
        Perfil perfil = perfiles.buscarPorUserId("u1").orElseThrow();
        assertEquals(ZoneId.of("America/Argentina/Buenos_Aires"), perfil.getZonaHoraria());
        assertEquals(Set.of(CanalNotificacion.EMAIL), perfil.getCanales());
    }

    @Test
    void unUsuarioTieneUnSoloPerfil() {
        service.crearPerfil("u1", comando("Ana", null));
        assertThrows(ReglaNegocioException.class, () -> service.crearPerfil("u1", comando("Otra", null)));
    }

    @Test
    void zonaHorariaInvalida() {
        assertThrows(ReglaNegocioException.class, () -> service.crearPerfil("u1", comando("Ana", "Marte/Base")));
    }

    @Test
    void actualizarYDarDeBaja() {
        service.crearPerfil("u1", comando("Ana", null));
        service.actualizarPerfil("u1", new PerfilCommand(null, "Ana Maria", null, " ", null, null, null, null));
        Perfil perfil = perfiles.buscarPorUserId("u1").orElseThrow();
        assertEquals("Ana Maria", perfil.getNombreVisible());
        assertTrue(perfil.getHabilidades().isEmpty());
        service.darDeBaja("u1");
        assertFalse(perfil.isActivo());
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarPerfil("nadie", comando("X", null)));
        assertThrows(RecursoNoEncontradoException.class, () -> service.darDeBaja("nadie"));
    }

    @Test
    void verPerfilPublicoConPublicacionesEHistorial() {
        Perfil prov = perfiles.guardar(Datos.profesional("prov"));
        Publicacion p = publicaciones.guardar(Datos.publicada(prov, 100, 1, null, INICIO));
        publicaciones.guardar(Datos.borrador(prov, null, 100, 1, null, List.of()));
        contrataciones.guardar(new Contratacion(p, perfiles.guardar(Datos.profesional("cli")),
                p.getFranjas().get(0), "m", "o", Datos.HOY));

        PerfilPublicoDto dto = service.verPerfil(prov.getId());
        assertEquals(1, dto.publicacionesActivas().size());
        assertEquals(List.of("Mentoria de entrevistas - PENDIENTE"), dto.historialResumido());
        assertEquals(prov.getId(), dto.id());
        assertThrows(RecursoNoEncontradoException.class, () -> service.verPerfil(99L));
    }

    @Test
    void verElPerfilDelPropioUsuario() {
        Perfil prov = perfiles.guardar(Datos.profesional("prov"));
        PerfilPublicoDto dto = service.verPerfilDeUsuario("prov");
        assertEquals(prov.getId(), dto.id());
        assertEquals("PROFESIONAL", dto.tipo());
        assertThrows(RecursoNoEncontradoException.class, () -> service.verPerfilDeUsuario("nadie"));
    }
}
