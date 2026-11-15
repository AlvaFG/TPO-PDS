package ar.edu.uade.skillswap.infrastructure.adapter;

import ar.edu.uade.skillswap.application.port.ICanalNotificacion;
import ar.edu.uade.skillswap.domain.model.Notificacion;

/*
 * Adapter Pattern - Adapter: traduce enviar(notificacion) a push(deviceId, payload).
 * El dispositivo se simula a partir del userId (no hay registro real de dispositivos).
 */
public class PushAdapter implements ICanalNotificacion {

    private final PushClientMock cliente;

    public PushAdapter(PushClientMock cliente) {
        this.cliente = cliente;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        String payload = "{\"evento\":\"" + notificacion.getTipoEvento() + "\",\"mensaje\":\""
                + notificacion.getMensaje().replace("\"", "'") + "\"}";
        int estado = cliente.push("device-" + notificacion.getDestinatarioId(), payload);
        if (estado != PushClientMock.OK) {
            throw new IllegalStateException("El proveedor de push respondio " + estado);
        }
    }
}
