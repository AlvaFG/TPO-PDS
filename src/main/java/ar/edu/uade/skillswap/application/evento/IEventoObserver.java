package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.domain.model.EventoContratacion;

/* Observer Pattern - Observador. */
public interface IEventoObserver {

    void actualizar(EventoContratacion evento);
}
