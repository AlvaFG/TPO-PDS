package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.Publicacion;

import java.util.List;

public record PublicacionDto(Long id, String titulo, String categoria, String modalidad, long precio,
                             double reputacionAutor, String estado, String descripcion, int duracionMinutos,
                             String autor, List<String> etiquetas, Long autorId) {

    public static PublicacionDto desde(Publicacion p) {
        return new PublicacionDto(p.getId(), p.getTitulo(),
                p.getCategoria() == null ? null : p.getCategoria().getNombre(),
                p.getModalidad() == null ? null : p.getModalidad().name(),
                p.getPrecioBase() == null ? 0 : p.getPrecioBase().getCantidad(),
                p.getAutor().getReputacion(), p.getNombreEstado(), p.getTextoDescripcion(),
                p.getDuracionMinutos(), p.getAutor().getNombreVisible(), p.getEtiquetas().stream().sorted().toList(),
                p.getAutor().getId());
    }
}
