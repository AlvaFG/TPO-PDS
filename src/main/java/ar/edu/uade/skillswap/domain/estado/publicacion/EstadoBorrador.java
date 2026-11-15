package ar.edu.uade.skillswap.domain.estado.publicacion;

import ar.edu.uade.skillswap.domain.model.Publicacion;

public class EstadoBorrador extends EstadoPublicacionBase {

    public static final String NOMBRE = "BORRADOR";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public void publicar(Publicacion p, String actorId) {
        p.validarPublicable();
        p.cambiarEstado(new EstadoPublicada(), actorId, "Publicada");
    }

    @Override
    public void cancelar(Publicacion p, String actorId, String motivo) {
        p.cambiarEstado(new EstadoPublicacionCancelada(), actorId, motivo);
    }

    @Override
    public boolean esEditable() {
        return true;
    }

    /* Un borrador no es visible: solo lo ve su autor. */
    @Override
    public boolean esVisiblePara(Publicacion p, String userId) {
        return p.esAutor(userId);
    }
}
