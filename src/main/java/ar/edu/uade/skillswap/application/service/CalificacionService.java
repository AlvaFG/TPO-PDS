package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.CalificarCommand;
import ar.edu.uade.skillswap.application.evento.IPublicadorEventos;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.ICalificacionRepository;
import ar.edu.uade.skillswap.application.port.IContratacionRepository;
import ar.edu.uade.skillswap.application.port.IGestorTransacciones;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.TipoEvento;

/*
 * Registra la calificacion y publica CALIFICACION_REGISTRADA:
 * ReputacionObserver recalcula la reputacion.
 */
public class CalificacionService implements ICalificacionService {

    private final IContratacionRepository contratacionRepository;
    private final ICalificacionRepository calificacionRepository;
    private final IGestorTransacciones transacciones;
    private final IPublicadorEventos eventos;

    public CalificacionService(IContratacionRepository contratacionRepository,
                               ICalificacionRepository calificacionRepository,
                               IGestorTransacciones transacciones, IPublicadorEventos eventos) {
        this.contratacionRepository = contratacionRepository;
        this.calificacionRepository = calificacionRepository;
        this.transacciones = transacciones;
        this.eventos = eventos;
    }

    @Override
    public void calificar(String userId, CalificarCommand cmd) {
        Contratacion contratacion = transacciones.ejecutar(() -> {
            Contratacion c = contratacionRepository.buscarPorId(cmd.contratacionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la contratacion " + cmd.contratacionId()));
            if (!c.esParticipante(userId)) {
                throw new AccesoDenegadoException("Solo los participantes pueden calificar la contratacion");
            }
            calificacionRepository.guardar(c.calificar(userId, cmd.puntaje(), cmd.comentario()));
            return contratacionRepository.guardar(c);
        });
        eventos.publicar(EventoContratacion.de(contratacion, TipoEvento.CALIFICACION_REGISTRADA));
    }
}
