package ar.edu.uade.skillswap.infrastructure.adapter;

import ar.edu.uade.skillswap.application.port.ICanalNotificacion;
import ar.edu.uade.skillswap.application.port.ICanalNotificacionFactory;
import ar.edu.uade.skillswap.application.port.INotificacionRepository;
import ar.edu.uade.skillswap.application.port.IServicioIdentidad;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;

/*
 * Simple Factory: un unico switch sobre CanalNotificacion.
 * Agregar un canal toca solo esta clase y el adapter nuevo; Application no conoce los adapters.
 */
public class CanalNotificacionFactory implements ICanalNotificacionFactory {

    private final EmailClientMock emailClient;
    private final PushClientMock pushClient;
    private final IServicioIdentidad identidad;
    private final INotificacionRepository notificacionRepository;

    public CanalNotificacionFactory(EmailClientMock emailClient, PushClientMock pushClient,
                                    IServicioIdentidad identidad, INotificacionRepository notificacionRepository) {
        this.emailClient = emailClient;
        this.pushClient = pushClient;
        this.identidad = identidad;
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public ICanalNotificacion crear(CanalNotificacion canal) {
        return switch (canal) {
            case EMAIL -> new EmailAdapter(emailClient, identidad);
            case PUSH -> new PushAdapter(pushClient);
            case INTERNA -> new CanalInterno(notificacionRepository);
        };
    }
}
