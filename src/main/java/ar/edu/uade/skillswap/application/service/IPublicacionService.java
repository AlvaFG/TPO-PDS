package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.CambiarEstadoCommand;
import ar.edu.uade.skillswap.application.dto.PublicacionCommand;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;

import java.util.List;

/* Puerto de entrada: casos de uso de Publicacion. */
public interface IPublicacionService {

    /* Devuelve el id de la publicacion creada en BORRADOR. */
    Long crearBorrador(String userId, PublicacionCommand cmd);

    void editar(String userId, Long publicacionId, PublicacionCommand cmd);

    void cambiarEstado(String userId, CambiarEstadoCommand cmd);

    /* Todas las publicaciones del usuario, en cualquier estado. */
    List<PublicacionDto> listarPropias(String userId);
}
