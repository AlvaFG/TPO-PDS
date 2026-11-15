package ar.edu.uade.skillswap.infrastructure.adapter;

import ar.edu.uade.skillswap.application.port.ICanalNotificacion;
import ar.edu.uade.skillswap.application.port.INotificacionRepository;
import ar.edu.uade.skillswap.domain.model.Notificacion;

/*
 * Canal de notificacion interno: no adapta a ningun proveedor, guarda la notificacion en la bandeja.
 */
public class CanalInterno implements ICanalNotificacion {

    private final INotificacionRepository notificacionRepository;

    public CanalInterno(INotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        notificacionRepository.guardar(notificacion);
    }
}
