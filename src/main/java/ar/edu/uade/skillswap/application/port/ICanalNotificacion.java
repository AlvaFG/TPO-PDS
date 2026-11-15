package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.domain.model.Notificacion;

/*
 * Adapter Pattern - Target de los proveedores de email y push, y contrato del canal interno.
 */
public interface ICanalNotificacion {

    void enviar(Notificacion notificacion);
}
