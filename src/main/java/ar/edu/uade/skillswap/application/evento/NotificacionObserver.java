package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.application.port.ICanalNotificacionFactory;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.Notificacion;
import ar.edu.uade.skillswap.domain.model.Perfil;

/*
 * Observer concreto: por cada destinatario del evento arma la notificacion
 * y la envia por cada canal que el usuario eligio (email, push y/o interna).
 */
public class NotificacionObserver implements IEventoObserver {

    private final IPerfilRepository perfilRepository;
    private final ICanalNotificacionFactory canalFactory;

    public NotificacionObserver(IPerfilRepository perfilRepository, ICanalNotificacionFactory canalFactory) {
        this.perfilRepository = perfilRepository;
        this.canalFactory = canalFactory;
    }

    @Override
    public void actualizar(EventoContratacion evento) {
        for (String destinatario : evento.destinatarios()) {
            perfilRepository.buscarPorUserId(destinatario).ifPresent(perfil -> notificar(perfil, evento));
        }
    }

    private void notificar(Perfil perfil, EventoContratacion evento) {
        String mensaje = "Contratacion " + evento.getContratacionId() + ": "
                + evento.getTipo().name().toLowerCase().replace('_', ' ');
        for (CanalNotificacion canal : perfil.getCanales()) {
            Notificacion notificacion = new Notificacion(perfil.getUserId(), canal, evento.getTipo(), mensaje, evento.getFecha());
            canalFactory.crear(canal).enviar(notificacion);
        }
    }
}
