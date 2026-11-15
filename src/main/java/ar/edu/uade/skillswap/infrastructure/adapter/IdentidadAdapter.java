package ar.edu.uade.skillswap.infrastructure.adapter;

import ar.edu.uade.skillswap.application.port.IServicioIdentidad;

import java.util.Map;

/*
 * Adapter Pattern - Adapter: traduce IServicioIdentidad (Target) a la API del proveedor (fetchUser).
 * Cambiar el mock por un proveedor real es escribir otro adapter, sin tocar Application.
 */
public class IdentidadAdapter implements IServicioIdentidad {

    private final IdentityClientMock cliente;

    public IdentidadAdapter(IdentityClientMock cliente) {
        this.cliente = cliente;
    }

    @Override
    public boolean validarUsuario(String userId) {
        Map<String, String> usuario = cliente.fetchUser(userId);
        return usuario != null && "ACTIVE".equals(usuario.get("status"));
    }

    /* null si el proveedor no conoce al usuario. */
    @Override
    public String obtenerEmail(String userId) {
        Map<String, String> usuario = cliente.fetchUser(userId);
        return usuario == null ? null : usuario.get("email");
    }
}
