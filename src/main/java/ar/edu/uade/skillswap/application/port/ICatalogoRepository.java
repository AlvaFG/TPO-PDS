package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;

import java.util.List;
import java.util.Optional;

public interface ICatalogoRepository {

    /* Devuelve la categoria con sus hijos cargados (se recorre con el Composite). */
    Optional<Categoria> buscarCategoria(Long id);

    List<Habilidad> buscarHabilidades(List<Long> ids);

    /* Todas las categorias (la UI lista sus nombres en los filtros). */
    List<Categoria> listarCategorias();
}
