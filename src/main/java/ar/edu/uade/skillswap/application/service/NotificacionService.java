package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.NotificacionDto;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.INotificacionRepository;
import ar.edu.uade.skillswap.domain.model.Notificacion;

public class NotificacionService implements INotificacionService {

    private final INotificacionRepository notificacionRepository;

    public NotificacionService(INotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public Pagina<NotificacionDto> listar(String userId, int pagina, int tamanio) {
        return notificacionRepository.listarPorDestinatario(userId, pagina, tamanio).map(NotificacionDto::desde);
    }

    @Override
    public void marcarLeida(String userId, Long notificacionId) {
        Notificacion notificacion = notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la notificacion " + notificacionId));
        if (!notificacion.getDestinatarioId().equals(userId)) {
            throw new AccesoDenegadoException("La notificacion no es del usuario");
        }
        notificacion.marcarLeida(userId);
        notificacionRepository.guardar(notificacion);
    }
}
