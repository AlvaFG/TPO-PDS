package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.CategoriaDto;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.ICatalogoRepository;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;

import java.util.List;

public class BusquedaService implements IBusquedaService {

    private final IPublicacionRepository publicacionRepository;
    private final ICatalogoRepository catalogoRepository;

    public BusquedaService(IPublicacionRepository publicacionRepository, ICatalogoRepository catalogoRepository) {
        this.publicacionRepository = publicacionRepository;
        this.catalogoRepository = catalogoRepository;
    }

    /* Buscar por una categoria incluye sus subcategorias (Composite). */
    @Override
    public Pagina<PublicacionDto> buscar(FiltroBusqueda filtro) {
        return publicacionRepository.buscar(filtro, categoriaIds(filtro.categoriaId()))
                .map(PublicacionDto::desde);
    }

    @Override
    public List<CategoriaDto> listarCategorias() {
        return catalogoRepository.listarCategorias().stream().map(CategoriaDto::desde).toList();
    }

    private List<Long> categoriaIds(Long categoriaId) {
        if (categoriaId == null) {
            return List.of();
        }
        return catalogoRepository.buscarCategoria(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoria " + categoriaId))
                .listarSubcategoriaIds();
    }
}
