package ar.edu.uade.skillswap.domain.estado.publicacion;

import ar.edu.uade.skillswap.domain.model.Publicacion;

public class EstadoCerrada extends EstadoPublicacionBase {

    public static final String NOMBRE = "CERRADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    /* Finalizada: no puede reabrirse. Solo la ve su autor. */
    @Override
    public boolean esVisiblePara(Publicacion p, String userId) {
        return p.esAutor(userId);
    }
}
