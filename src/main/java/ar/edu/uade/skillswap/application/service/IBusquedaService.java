package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.CategoriaDto;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;

import java.util.List;

/* Puerto de entrada: busqueda de publicaciones (lado consulta). */
public interface IBusquedaService {

    Pagina<PublicacionDto> buscar(FiltroBusqueda filtro);

    /* Categorias del catalogo, para los filtros de la UI. */
    List<CategoriaDto> listarCategorias();
}
