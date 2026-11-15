package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.dto.CategoriaDto;
import ar.edu.uade.skillswap.application.dto.CriterioOrden;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.service.IBusquedaService;
import ar.edu.uade.skillswap.application.service.IRecomendacionService;
import ar.edu.uade.skillswap.controllers.UiControllers.BusquedaController;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.util.ArrayList;
import java.util.List;

public class BusquedaControllerImpl implements BusquedaController {

    static final String TODAS = "Todas";
    /* La UI no pagina: se pide una unica pagina lo bastante grande. */
    private static final int TAMANIO_PAGINA = 1000;
    private static final int CANTIDAD_RECOMENDADAS = 3;

    private final IBusquedaService busqueda;
    private final IRecomendacionService recomendacion;
    private final EjecutorDeAcciones ejecutor;

    public BusquedaControllerImpl(IBusquedaService busqueda, IRecomendacionService recomendacion,
                                  EjecutorDeAcciones ejecutor) {
        this.busqueda = busqueda;
        this.recomendacion = recomendacion;
        this.ejecutor = ejecutor;
    }

    @Override
    public List<Publicacion> buscar(FiltrosBusqueda f) {
        Long categoriaId = categoriaId(f.categoria());
        FiltroBusqueda filtro = new FiltroBusqueda(
                f.texto() == null || f.texto().isBlank() ? null : f.texto().trim(),
                categoriaId, f.modalidad() == null ? null : ModalidadUi.aDominio(f.modalidad()),
                null, f.precioMaximo() > 0 ? (long) f.precioMaximo() : null, null, null,
                orden(f.orden()), 0, TAMANIO_PAGINA);
        return busqueda.buscar(filtro).contenido().stream().map(Conversor::publicacion).toList();
    }

    @Override
    public List<Publicacion> recomendar(String userId) {
        return ejecutor.ejecutar(userId, () -> recomendacion.recomendar(userId, 0, CANTIDAD_RECOMENDADAS)
                .contenido().stream().map(Conversor::publicacion).toList());
    }

    @Override
    public List<String> categorias() {
        List<String> nombres = new ArrayList<>(List.of(TODAS));
        busqueda.listarCategorias().stream().map(CategoriaDto::nombre).forEach(nombres::add);
        return nombres;
    }

    /* "Todas" (o sin categoria) no filtra; el resto se resuelve por nombre. */
    private Long categoriaId(String nombre) {
        if (nombre == null || TODAS.equals(nombre)) {
            return null;
        }
        return busqueda.listarCategorias().stream().filter(c -> c.nombre().equals(nombre)).findFirst()
                .map(CategoriaDto::id)
                .orElseThrow(() -> new ErrorDeAplicacion("No existe la categoria " + nombre));
    }

    private CriterioOrden orden(String orden) {
        if ("Menor precio".equals(orden)) {
            return CriterioOrden.MENOR_PRECIO;
        }
        if ("Mejor reputación".equals(orden)) {
            return CriterioOrden.MEJOR_REPUTACION;
        }
        return CriterioOrden.RELEVANCIA;
    }
}
