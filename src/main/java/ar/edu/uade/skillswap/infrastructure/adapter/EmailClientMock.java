package ar.edu.uade.skillswap.infrastructure.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Adapter Pattern - Adaptee: simula el SDK de un proveedor de email. No usa la red:
 * guarda cada correo en una lista y lo escribe en consola.
 */
public class EmailClientMock {

    public record Correo(String to, String subject, String body) {
    }

    private final List<Correo> enviados = new ArrayList<>();

    public void sendMail(String to, String subject, String body) {
        enviados.add(new Correo(to, subject, body));
        System.out.println("[email] para " + to + " | " + subject + " | " + body);
    }

    public List<Correo> getEnviados() {
        return Collections.unmodifiableList(enviados);
    }
}
