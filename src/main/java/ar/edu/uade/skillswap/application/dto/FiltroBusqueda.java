package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.Modalidad;

/*
 * Filtros de la busqueda. Los campos null no filtran.
 */
public record FiltroBusqueda(String texto, Long categoriaId, Modalidad modalidad, Long precioMinimo,
                             Long precioMaximo, Long habilidadId, Double reputacionMinima,
                             CriterioOrden orden, int pagina, int tamanio) {
}
