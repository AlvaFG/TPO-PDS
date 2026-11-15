package ar.edu.uade.skillswap.domain.estado.publicacion;

import ar.edu.uade.skillswap.domain.model.Publicacion;

/*
 * State Pattern - Estado de una publicacion (consigna 4.2).
 */
public interface EstadoPublicacion {

    String getNombre();

    void publicar(Publicacion p, String actorId);

    void pausar(Publicacion p, String actorId);

    void reanudar(Publicacion p, String actorId);

    void cerrar(Publicacion p, String actorId);

    void cancelar(Publicacion p, String actorId, String motivo);

    boolean esEditable();

    boolean admiteSolicitudes();

    boolean esVisiblePara(Publicacion p, String userId);
}
