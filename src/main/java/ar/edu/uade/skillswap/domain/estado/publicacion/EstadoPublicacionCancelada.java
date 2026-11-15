package ar.edu.uade.skillswap.domain.estado.publicacion;

import ar.edu.uade.skillswap.domain.model.Publicacion;

public class EstadoPublicacionCancelada extends EstadoPublicacionBase {

    public static final String NOMBRE = "CANCELADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public boolean esVisiblePara(Publicacion p, String userId) {
        return p.esAutor(userId);
    }
}
