package ar.edu.uade.skillswap.domain.estado.publicacion;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Publicacion;

/*
 * State Pattern - Estado base.
 * Por defecto toda transicion es invalida; cada estado concreto redefine solo lo que permite.
 */
public abstract class EstadoPublicacionBase implements EstadoPublicacion {

    @Override
    public void publicar(Publicacion p, String actorId) {
        throw transicionInvalida("publicar");
    }

    @Override
    public void pausar(Publicacion p, String actorId) {
        throw transicionInvalida("pausar");
    }

    @Override
    public void reanudar(Publicacion p, String actorId) {
        throw transicionInvalida("reanudar");
    }

    @Override
    public void cerrar(Publicacion p, String actorId) {
        throw transicionInvalida("cerrar");
    }

    @Override
    public void cancelar(Publicacion p, String actorId, String motivo) {
        throw transicionInvalida("cancelar");
    }

    @Override
    public boolean esEditable() {
        return false;
    }

    @Override
    public boolean admiteSolicitudes() {
        return false;
    }

    @Override
    public boolean esVisiblePara(Publicacion p, String userId) {
        return false;
    }

    private ReglaNegocioException transicionInvalida(String accion) {
        return new ReglaNegocioException("Transicion invalida: no se puede " + accion
                + " una publicacion en estado " + getNombre());
    }
}
