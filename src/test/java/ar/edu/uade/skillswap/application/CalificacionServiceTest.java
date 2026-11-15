package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.dto.CalificarCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.evento.ReputacionObserver;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.service.CalificacionService;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

/* Caso de aceptacion 10: completar, calificar una sola vez y recalcular la reputacion. */
class CalificacionServiceTest {

    private final Repositorios.Perfiles perfiles = new Repositorios.Perfiles();
    private final Repositorios.Contrataciones contrataciones = new Repositorios.Contrataciones();
    private final Repositorios.Calificaciones calificaciones = new Repositorios.Calificaciones();
    private CalificacionService service;
    private Perfil proveedor;
    private Perfil cliente;
    private Contratacion contratacion;

    @BeforeEach
    void preparar() {
        PublicadorEventos publicador = new PublicadorEventos();
        publicador.suscribir(new ReputacionObserver(perfiles, calificaciones));
        service = new CalificacionService(contrataciones, calificaciones, new Repositorios.GestorTransacciones(), publicador);

        proveedor = perfiles.guardar(Datos.profesional("prov"));
        cliente = perfiles.guardar(Datos.profesional("cli"));
        perfiles.guardar(Datos.profesional("otro"));
        Publicacion p = Datos.publicada(proveedor, 100, 1, null, INICIO);
        contratacion = contrataciones.guardar(new Contratacion(p, cliente, p.getFranjas().get(0), "m", "o", Datos.HOY));
    }

    private void completar() {
        contratacion.aceptar("prov");
        contratacion.pagar("cli", new Billetera(cliente, new SkillCoins(100)), new Billetera(proveedor, SkillCoins.CERO));
        contratacion.iniciar("prov");
        contratacion.completar("prov");
    }

    @Test
    void calificarRecalculaLaReputacionDelCalificado() {
        completar();
        service.calificar("cli", new CalificarCommand(contratacion.getId(), 4, "Muy buena"));
        assertEquals(4.0, proveedor.getReputacion());
        assertEquals(1, proveedor.getCantidadCalificaciones());
        assertEquals(0, cliente.getReputacion());

        service.calificar("prov", new CalificarCommand(contratacion.getId(), 5, "Puntual"));
        assertEquals(5.0, cliente.getReputacion());
        assertEquals(2, calificaciones.todas().size());
    }

    @Test
    void seCalificaUnaSolaVez() {
        completar();
        service.calificar("cli", new CalificarCommand(contratacion.getId(), 4, "Muy buena"));
        assertThrows(ReglaNegocioException.class,
                () -> service.calificar("cli", new CalificarCommand(contratacion.getId(), 1, "Cambio de opinion")));
        assertEquals(4.0, proveedor.getReputacion());
        assertEquals(1, calificaciones.todas().size());
    }

    @Test
    void calificacionNoAutorizada() {
        assertThrows(ReglaNegocioException.class,
                () -> service.calificar("cli", new CalificarCommand(contratacion.getId(), 5, "Sin completar")));
        completar();
        assertThrows(AccesoDenegadoException.class,
                () -> service.calificar("otro", new CalificarCommand(contratacion.getId(), 5, "Intruso")));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.calificar("cli", new CalificarCommand(99L, 5, "x")));
        assertTrue(calificaciones.todas().isEmpty());
    }
}
