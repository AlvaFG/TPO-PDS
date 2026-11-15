package ar.edu.uade.skillswap.application.dto;

/*
 * motivo es obligatorio para CANCELAR.
 */
public record CambiarEstadoCommand(Long publicacionId, AccionPublicacion accion, String motivo) {
}
