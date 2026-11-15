package ar.edu.uade.skillswap.domain.estado.publicacion;

import ar.edu.uade.skillswap.domain.model.Publicacion;

public class EstadoPublicada extends EstadoPublicacionBase {

    public static final String NOMBRE = "PUBLICADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public void pausar(Publicacion p, String actorId) {
        p.cambiarEstado(new EstadoPausada(), actorId, "Pausada");
    }

    @Override
    public void cerrar(Publicacion p, String actorId) {
        p.cambiarEstado(new EstadoCerrada(), actorId, "Cerrada");
    }

    @Override
    public void cancelar(Publicacion p, String actorId, String motivo) {
        p.cambiarEstado(new EstadoPublicacionCancelada(), actorId, motivo);
    }

    @Override
    public boolean admiteSolicitudes() {
        return true;
    }

    @Override
    public boolean esVisiblePara(Publicacion p, String userId) {
        return true;
    }
}
