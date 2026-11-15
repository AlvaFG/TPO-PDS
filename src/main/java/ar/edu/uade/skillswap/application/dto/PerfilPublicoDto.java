package ar.edu.uade.skillswap.application.dto;

import java.util.List;

public record PerfilPublicoDto(Long id, String tipo, String nombreVisible, String biografia, double reputacion,
                               int cantidadCalificaciones, List<String> habilidades,
                               List<PublicacionDto> publicacionesActivas, List<String> historialResumido) {
}
