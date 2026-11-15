package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.evento.IEventoObserver;
import ar.edu.uade.skillswap.application.evento.NotificacionObserver;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.evento.ReputacionObserver;
import ar.edu.uade.skillswap.application.port.ICalificacionRepository;
import ar.edu.uade.skillswap.application.port.ICanalNotificacion;
import ar.edu.uade.skillswap.application.port.ICanalNotificacionFactory;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.Notificacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.TipoEvento;
import ar.edu.uade.skillswap.soporte.Datos;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/* Observer y caso de aceptacion 8: dos tipos de notificacion ante un mismo evento. */
class ObserverTest {

    private final EventoContratacion solicitud =
            new EventoContratacion(TipoEvento.SOLICITUD_RECIBIDA, 7L, "cli", "prov", INICIO);

    @Test
    void unMismoEventoSaleporDosCanalesMock() {
        Perfil proveedor = Datos.profesional("prov");
        proveedor.elegirCanales(Set.of(CanalNotificacion.EMAIL, CanalNotificacion.INTERNA));
        IPerfilRepository perfiles = mock(IPerfilRepository.class);
        when(perfiles.buscarPorUserId("prov")).thenReturn(Optional.of(proveedor));
        ICanalNotificacion email = mock(ICanalNotificacion.class);
        ICanalNotificacion interna = mock(ICanalNotificacion.class);
        ICanalNotificacionFactory factory = mock(ICanalNotificacionFactory.class);
        when(factory.crear(CanalNotificacion.EMAIL)).thenReturn(email);
        when(factory.crear(CanalNotificacion.INTERNA)).thenReturn(interna);

        PublicadorEventos publicador = new PublicadorEventos();
        publicador.suscribir(new NotificacionObserver(perfiles, factory));
        publicador.publicar(solicitud);

        ArgumentCaptor<Notificacion> enviada = ArgumentCaptor.forClass(Notificacion.class);
        verify(email).enviar(enviada.capture());
        verify(interna).enviar(any(Notificacion.class));
        assertEquals("prov", enviada.getValue().getDestinatarioId());
        assertEquals(CanalNotificacion.EMAIL, enviada.getValue().getCanal());
        assertEquals("Contratacion 7: solicitud recibida", enviada.getValue().getMensaje());
        verify(factory, never()).crear(CanalNotificacion.PUSH);
    }

    @Test
    void destinatarioSinPerfilNoRecibeNada() {
        IPerfilRepository perfiles = mock(IPerfilRepository.class);
        when(perfiles.buscarPorUserId("prov")).thenReturn(Optional.empty());
        ICanalNotificacionFactory factory = mock(ICanalNotificacionFactory.class);
        new NotificacionObserver(perfiles, factory).actualizar(solicitud);
        verifyNoInteractions(factory);
    }

    @Test
    void elSujetoAvisaATodosLosSuscriptosHastaQueSeDesuscriben() {
        IEventoObserver primero = mock(IEventoObserver.class);
        IEventoObserver segundo = mock(IEventoObserver.class);
        PublicadorEventos publicador = new PublicadorEventos();
        publicador.suscribir(primero);
        publicador.suscribir(segundo);
        publicador.publicar(solicitud);
        publicador.desuscribir(segundo);
        publicador.publicar(solicitud);
        verify(primero, times(2)).actualizar(solicitud);
        verify(segundo, times(1)).actualizar(solicitud);
    }

    @Test
    void reputacionIgnoraLosEventosQueNoSonCalificaciones() {
        IPerfilRepository perfiles = mock(IPerfilRepository.class);
        ICalificacionRepository calificaciones = mock(ICalificacionRepository.class);
        new ReputacionObserver(perfiles, calificaciones).actualizar(solicitud);
        verifyNoInteractions(perfiles, calificaciones);
    }
}
