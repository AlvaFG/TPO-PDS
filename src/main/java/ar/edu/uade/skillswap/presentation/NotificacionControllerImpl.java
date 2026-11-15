package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.dto.NotificacionDto;
import ar.edu.uade.skillswap.application.service.INotificacionService;
import ar.edu.uade.skillswap.controllers.UiControllers.NotificacionController;
import ar.edu.uade.skillswap.model.Models.Notificacion;

import java.util.List;

public class NotificacionControllerImpl implements NotificacionController {

    /* La UI no pagina: se pide una unica pagina lo bastante grande. */
    private static final int TAMANIO_PAGINA = 1000;

    private final INotificacionService notificaciones;
    private final EjecutorDeAcciones ejecutor;

    public NotificacionControllerImpl(INotificacionService notificaciones, EjecutorDeAcciones ejecutor) {
        this.notificaciones = notificaciones;
        this.ejecutor = ejecutor;
    }

    @Override
    public List<Notificacion> listarNotificaciones(String userId) {
        return ejecutor.ejecutar(userId, () -> notificaciones.listar(userId, 0, TAMANIO_PAGINA)
                .contenido().stream().map(Conversor::notificacion).toList());
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        ejecutor.ejecutar(userId, () -> notificaciones.marcarLeida(userId, Conversor.id(notificacionId)));
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        ejecutor.ejecutar(userId, () -> notificaciones.listar(userId, 0, TAMANIO_PAGINA).contenido().stream()
                .filter(n -> !n.leida()).map(NotificacionDto::id)
                .forEach(id -> notificaciones.marcarLeida(userId, id)));
    }
}
