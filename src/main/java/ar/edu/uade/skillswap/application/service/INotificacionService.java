package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.NotificacionDto;
import ar.edu.uade.skillswap.application.dto.Pagina;

/* Puerto de entrada: bandeja de notificaciones internas. */
public interface INotificacionService {

    Pagina<NotificacionDto> listar(String userId, int pagina, int tamanio);

    void marcarLeida(String userId, Long notificacionId);
}
