package ar.edu.uade.skillswap.infrastructure.adapter;

import ar.edu.uade.skillswap.application.port.ICanalNotificacion;
import ar.edu.uade.skillswap.application.port.IServicioIdentidad;
import ar.edu.uade.skillswap.domain.model.Notificacion;

/*
 * Adapter Pattern - Adapter: traduce enviar(notificacion) a sendMail(to, subject, body).
 * El email del destinatario lo resuelve el servicio de identidad (la aplicacion solo conoce el userId).
 */
public class EmailAdapter implements ICanalNotificacion {

    private final EmailClientMock cliente;
    private final IServicioIdentidad identidad;

    public EmailAdapter(EmailClientMock cliente, IServicioIdentidad identidad) {
        this.cliente = cliente;
        this.identidad = identidad;
    }

    /* Si identidad no conoce un email para el usuario, no hay a quien enviarle el correo. */
    @Override
    public void enviar(Notificacion notificacion) {
        String email = identidad.obtenerEmail(notificacion.getDestinatarioId());
        if (email == null) {
            return;
        }
        cliente.sendMail(email, "SkillSwap - " + asunto(notificacion), notificacion.getMensaje());
    }

    private static String asunto(Notificacion notificacion) {
        return notificacion.getTipoEvento().name().toLowerCase().replace('_', ' ');
    }
}
