package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.domain.model.Publicacion;

import java.util.List;
import java.util.Optional;

/* Puerto de salida de persistencia de publicaciones. */
public interface IPublicacionRepository {

    Publicacion guardar(Publicacion publicacion);

    Optional<Publicacion> buscarPorId(Long id);

    /*
     * Solo publicaciones PUBLICADAS que cumplan el filtro, ordenadas por filtro.orden()
     * y paginadas con filtro.pagina()/filtro.tamanio().
     * categoriaIds: categoria pedida y sus subcategorias (vacia = sin filtro de categoria).
     */
    Pagina<Publicacion> buscar(FiltroBusqueda filtro, List<Long> categoriaIds);

    /* Publicaciones del autor en estado PUBLICADA o PAUSADA. */
    List<Publicacion> listarActivasDe(Long autorId);

    List<Publicacion> listarPublicadas();

    /*
     * Todas las publicaciones del autor, borradores incluidos (la UI "Mis publicaciones").
     * Por defecto devuelve las activas; la implementacion de persistencia debe sobreescribirlo.
     */
    default List<Publicacion> listarDeAutor(Long autorId) {
        return listarActivasDe(autorId);
    }
}
