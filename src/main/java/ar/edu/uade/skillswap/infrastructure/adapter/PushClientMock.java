package ar.edu.uade.skillswap.infrastructure.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Adapter Pattern - Adaptee: simula el SDK de un proveedor de notificaciones push. No usa la red:
 * guarda cada envio en una lista, lo escribe en consola y responde un codigo de estado.
 */
public class PushClientMock {

    public static final int OK = 200;

    public record Push(String deviceId, String payload) {
    }

    private final List<Push> enviados = new ArrayList<>();

    public int push(String deviceId, String payload) {
        enviados.add(new Push(deviceId, payload));
        System.out.println("[push] " + deviceId + " | " + payload);
        return OK;
    }

    public List<Push> getEnviados() {
        return Collections.unmodifiableList(enviados);
    }
}
