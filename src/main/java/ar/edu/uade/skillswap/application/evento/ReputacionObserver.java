package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.application.port.ICalificacionRepository;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.TipoEvento;

import java.util.List;

/*
 * Observer concreto: ante CALIFICACION_REGISTRADA recalcula la reputacion de las dos partes
 * a partir de sus calificaciones recibidas.
 */
public class ReputacionObserver implements IEventoObserver {

    private final IPerfilRepository perfilRepository;
    private final ICalificacionRepository calificacionRepository;

    public ReputacionObserver(IPerfilRepository perfilRepository, ICalificacionRepository calificacionRepository) {
        this.perfilRepository = perfilRepository;
        this.calificacionRepository = calificacionRepository;
    }

    @Override
    public void actualizar(EventoContratacion evento) {
        if (evento.getTipo() != TipoEvento.CALIFICACION_REGISTRADA) {
            return;
        }
        for (String userId : List.of(evento.getClienteId(), evento.getProveedorId())) {
            perfilRepository.buscarPorUserId(userId).ifPresent(perfil -> {
                perfil.recalcularReputacion(calificacionRepository.listarRecibidas(perfil.getId()));
                perfilRepository.guardar(perfil);
            });
        }
    }
}
