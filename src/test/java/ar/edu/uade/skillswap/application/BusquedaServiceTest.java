package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.dto.AccionPublicacion;
import ar.edu.uade.skillswap.application.dto.CambiarEstadoCommand;
import ar.edu.uade.skillswap.application.dto.CriterioOrden;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.PublicacionCommand;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.service.BusquedaService;
import ar.edu.uade.skillswap.application.service.PublicacionService;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.model.Modalidad;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

/* Caso de aceptacion 1: publicar una mentoria valida y encontrarla con una busqueda filtrada. */
class BusquedaServiceTest {

    private final Repositorios.Perfiles perfiles = new Repositorios.Perfiles();
    private final Repositorios.Publicaciones publicaciones = new Repositorios.Publicaciones();
    private final Repositorios.Catalogo catalogo = new Repositorios.Catalogo();
    private PublicacionService publicacionService;
    private BusquedaService busqueda;
    private Categoria tecnologia;
    private Categoria arquitectura;
    private Habilidad java;

    @BeforeEach
    void preparar() {
        publicacionService = new PublicacionService(publicaciones, perfiles, catalogo, new Repositorios.Politicas(),
                new Repositorios.GestorTransacciones(), Clock.systemDefaultZone());
        busqueda = new BusquedaService(publicaciones, catalogo);
        perfiles.guardar(Datos.profesional("prov"));
        tecnologia = catalogo.guardar(new Categoria("Tecnologia"));
        arquitectura = catalogo.guardar(new Categoria("Arquitectura"));
        java = catalogo.guardar(new Habilidad("Java"));
        tecnologia.agregar(arquitectura);
        arquitectura.agregar(java);

        publicar("Mentoria de microservicios", arquitectura, Modalidad.REMOTA, 500, List.of(java.getId()));
        publicar("Asesoria de redes", tecnologia, Modalidad.PRESENCIAL, 1500, List.of());
        publicacionService.crearBorrador("prov", comando("Borrador oculto", tecnologia, Modalidad.REMOTA, 100, List.of()));
    }

    private PublicacionCommand comando(String titulo, Categoria categoria, Modalidad modalidad, long precio,
                                       List<Long> habilidades) {
        return new PublicacionCommand(TipoPublicacion.MENTORIA, titulo, "d", categoria.getId(), habilidades,
                modalidad, 60, 1, precio, Set.of(), List.of(INICIO), null);
    }

    private void publicar(String titulo, Categoria categoria, Modalidad modalidad, long precio, List<Long> habilidades) {
        Long id = publicacionService.crearBorrador("prov", comando(titulo, categoria, modalidad, precio, habilidades));
        publicacionService.cambiarEstado("prov", new CambiarEstadoCommand(id, AccionPublicacion.PUBLICAR, null));
    }

    private FiltroBusqueda filtro(String texto, Long categoriaId, Modalidad modalidad, Long min, Long max,
                                  Long habilidadId, CriterioOrden orden, int tamanio) {
        return new FiltroBusqueda(texto, categoriaId, modalidad, min, max, habilidadId, null, orden, 0, tamanio);
    }

    private List<String> titulos(Pagina<PublicacionDto> pagina) {
        return pagina.contenido().stream().map(PublicacionDto::titulo).toList();
    }

    @Test
    void buscarPorCategoriaIncluyeLasSubcategorias() {
        Pagina<PublicacionDto> resultado = busqueda.buscar(filtro(null, tecnologia.getId(), null, null, null, null,
                CriterioOrden.MENOR_PRECIO, 10));
        assertEquals(List.of("Mentoria de microservicios", "Asesoria de redes"), titulos(resultado));
        PublicacionDto dto = resultado.contenido().get(0);
        assertEquals("Arquitectura", dto.categoria());
        assertEquals("REMOTA", dto.modalidad());
        assertEquals(500, dto.precio());
        assertEquals("PUBLICADA", dto.estado());
    }

    @Test
    void encuentraLaMentoriaConFiltrosCombinados() {
        Pagina<PublicacionDto> resultado = busqueda.buscar(filtro("micro", arquitectura.getId(), Modalidad.REMOTA,
                100L, 600L, java.getId(), CriterioOrden.RELEVANCIA, 10));
        assertEquals(List.of("Mentoria de microservicios"), titulos(resultado));
    }

    @Test
    void ordenaYPagina() {
        Pagina<PublicacionDto> primera = busqueda.buscar(filtro(null, null, null, null, null, null,
                CriterioOrden.MAS_RECIENTE, 1));
        assertEquals(2, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertEquals(1, primera.contenido().size());
    }

    @Test
    void categoriaInexistente() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> busqueda.buscar(filtro(null, 99L, null, null, null, null, null, 10)));
    }

    @Test
    void listaLasCategoriasDelCatalogo() {
        assertEquals(List.of("Tecnologia", "Arquitectura"),
                busqueda.listarCategorias().stream().map(c -> c.nombre()).toList());
    }
}
