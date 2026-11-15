package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.Notificacion;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.TipoEvento;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Ids;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

class EventoYNotificacionTest {

    private EventoContratacion evento(TipoEvento tipo) {
        return new EventoContratacion(tipo, 1L, "cli", "prov", INICIO);
    }

    @Test
    void cadaEventoSabeASusDestinatarios() {
        assertEquals(List.of("prov"), evento(TipoEvento.SOLICITUD_RECIBIDA).destinatarios());
        assertEquals(List.of("cli"), evento(TipoEvento.SOLICITUD_ACEPTADA).destinatarios());
        assertEquals(List.of("cli", "prov"), evento(TipoEvento.PAGO_CONFIRMADO).destinatarios());
        assertEquals(List.of("cli", "prov"), evento(TipoEvento.REPROGRAMACION).destinatarios());
        assertEquals(List.of("cli", "prov"), evento(TipoEvento.CANCELACION).destinatarios());
        assertEquals(List.of("cli", "prov"), evento(TipoEvento.SESION_COMPLETADA).destinatarios());
        assertTrue(evento(TipoEvento.CALIFICACION_REGISTRADA).destinatarios().isEmpty());
    }

    @Test
    void seCreaDesdeLaContratacion() {
        Publicacion p = Datos.publicada(Datos.profesional("prov"), 100, 1, null, INICIO);
        Contratacion c = Ids.asignar(new Contratacion(p, Datos.profesional("cli"), p.getFranjas().get(0), "m", "o", Datos.HOY), 9L);
        EventoContratacion e = EventoContratacion.de(c, TipoEvento.SOLICITUD_RECIBIDA);
        assertEquals(TipoEvento.SOLICITUD_RECIBIDA, e.getTipo());
        assertEquals(9L, e.getContratacionId());
        assertEquals("cli", e.getClienteId());
        assertEquals("prov", e.getProveedorId());
        assertNotNull(e.getFecha());
    }

    @Test
    void soloElDestinatarioMarcaLeida() {
        Notificacion n = new Notificacion("cli", CanalNotificacion.INTERNA, TipoEvento.PAGO_CONFIRMADO, "Pago", INICIO);
        assertThrows(ReglaNegocioException.class, () -> n.marcarLeida("otro"));
        assertFalse(n.isLeida());
        n.marcarLeida("cli");
        assertTrue(n.isLeida());
        assertEquals(CanalNotificacion.INTERNA, n.getCanal());
        assertEquals(TipoEvento.PAGO_CONFIRMADO, n.getTipoEvento());
        assertEquals("Pago", n.getMensaje());
        assertEquals(INICIO, n.getFecha());
        assertNull(n.getId());
    }
}
