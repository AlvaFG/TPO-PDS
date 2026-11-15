package ar.edu.uade.skillswap.infrastructure.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Adapter Pattern - Adaptee: simula el SDK del servicio de identidad externo (no se modifica).
 * Su API no coincide con la que necesita la aplicacion: devuelve el usuario como un mapa de atributos.
 * Trae cargados los usuarios de los datos de demostracion (V2__datos_demo.sql).
 */
public class IdentityClientMock {

    public static final Map<String, String> USUARIOS_DEMO = Map.of(
            "user-mateo", "mateo.silva@skillswap.test",
            "user-sofia", "sofia.mendez@skillswap.test",
            "user-tomas", "tomas.vidal@skillswap.test",
            "user-camila", "camila.torres@skillswap.test",
            "user-nicolas", "nicolas.paz@skillswap.test",
            "user-laura", "laura.rossi@skillswap.test",
            "user-martin", "martin.aguirre@skillswap.test",
            "user-acme", "contacto@acmelabs.test");

    private final Map<String, String> emails = new LinkedHashMap<>();

    public IdentityClientMock() {
        this(USUARIOS_DEMO);
    }

    /* userId -> email de los usuarios que el proveedor conoce. */
    public IdentityClientMock(Map<String, String> emailsPorUsuario) {
        emails.putAll(emailsPorUsuario);
    }

    /* Devuelve null si el usuario no existe en el proveedor. */
    public Map<String, String> fetchUser(String userId) {
        String email = userId == null ? null : emails.get(userId);
        if (email == null) {
            return null;
        }
        return Map.of("sub", userId, "email", email, "status", "ACTIVE");
    }
}
