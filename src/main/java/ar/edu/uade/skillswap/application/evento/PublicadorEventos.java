package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.domain.model.EventoContratacion;

import java.util.ArrayList;
import java.util.List;

/* Observer Pattern - Sujeto concreto: avisa el evento a cada observador suscripto. */
public class PublicadorEventos implements IPublicadorEventos {

    private final List<IEventoObserver> observers = new ArrayList<>();

    @Override
    public void suscribir(IEventoObserver observer) {
        observers.add(observer);
    }

    @Override
    public void desuscribir(IEventoObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void publicar(EventoContratacion evento) {
        observers.forEach(observer -> observer.actualizar(evento));
    }
}
