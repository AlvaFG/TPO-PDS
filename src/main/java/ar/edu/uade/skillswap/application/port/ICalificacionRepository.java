package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.domain.model.Calificacion;

import java.util.List;

public interface ICalificacionRepository {

    Calificacion guardar(Calificacion calificacion);

    /* Calificaciones cuyo calificado es el perfil. */
    List<Calificacion> listarRecibidas(Long perfilId);
}
