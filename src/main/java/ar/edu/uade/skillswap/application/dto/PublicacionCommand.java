package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.Modalidad;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/*
 * Datos de una publicacion. Cada inicio de inicioFranjas genera una franja de duracionMinutos.
 * categoriaId y politicaCancelacionId pueden ser null mientras es borrador.
 */
public record PublicacionCommand(TipoPublicacion tipo, String titulo, String descripcion, Long categoriaId,
                                 List<Long> habilidadIds, Modalidad modalidad, int duracionMinutos, int cupo,
                                 long precioBase, Set<String> etiquetas, List<LocalDateTime> inicioFranjas,
                                 Long politicaCancelacionId) {
}
