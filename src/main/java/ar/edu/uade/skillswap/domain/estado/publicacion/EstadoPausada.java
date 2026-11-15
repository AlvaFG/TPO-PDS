package ar.edu.uade.skillswap.domain.estado.publicacion;

import ar.edu.uade.skillswap.domain.model.Publicacion;

public class EstadoPausada extends EstadoPublicacionBase {

    public static final String NOMBRE = "PAUSADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public void reanudar(Publicacion p, String actorId) {
        p.cambiarEstado(new EstadoPublicada(), actorId, "Reanudada");
    }

    @Override
    public void cerrar(Publicacion p, String actorId) {
        p.cambiarEstado(new EstadoCerrada(), actorId, "Cerrada");
    }

    @Override
    public void cancelar(Publicacion p, String actorId, String motivo) {
        p.cambiarEstado(new EstadoPublicacionCancelada(), actorId, motivo);
    }

    /* Visible para su autor, pero no admite nuevas solicitudes. */
    @Override
    public boolean esVisiblePara(Publicacion p, String userId) {
        return p.esAutor(userId);
    }
}
