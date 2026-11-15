package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.domain.model.EventoContratacion;

/* Observer Pattern - Sujeto. Los servicios publican despues de confirmar la transaccion. */
public interface IPublicadorEventos {

    void suscribir(IEventoObserver observer);

    void desuscribir(IEventoObserver observer);

    void publicar(EventoContratacion evento);
}
