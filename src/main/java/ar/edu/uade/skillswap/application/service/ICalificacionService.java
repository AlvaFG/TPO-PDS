package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.CalificarCommand;

/* Puerto de entrada: calificacion verificada de una contratacion completada. */
public interface ICalificacionService {

    void calificar(String userId, CalificarCommand cmd);
}
