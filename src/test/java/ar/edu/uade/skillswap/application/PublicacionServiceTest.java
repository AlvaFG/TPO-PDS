package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.dto.AccionPublicacion;
import ar.edu.uade.skillswap.application.dto.CambiarEstadoCommand;
import ar.edu.uade.skillswap.application.dto.PublicacionCommand;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.service.PublicacionService;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Modalidad;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.domain.politica.PoliticaEstricta;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

class PublicacionServiceTest {

    private static final LocalDateTime AHORA = INICIO.minusDays(3);

    private final Repositorios.Perfiles perfiles = new Repositorios.Perfiles();
    private final Repositorios.Publicaciones publicaciones = new Repositorios.Publicaciones();
    private final Repositorios.Catalogo catalogo = new Repositorios.Catalogo();
    private final Repositorios.Politicas politicas = new Repositorios.Politicas();
    private PublicacionService service;
    private Categoria tecnologia;
    private Habilidad java;
    private Long politicaId;

    @BeforeEach
    void preparar() {
        ZoneId zona = ZoneId.systemDefault();
        service = new PublicacionService(publicaciones, perfiles, catalogo, politicas,
                new Repositorios.GestorTransacciones(), Clock.fixed(AHORA.atZone(zona).toInstant(), zona));
        perfiles.guardar(Datos.profesional("prov"));
        perfiles.guardar(Datos.profesional("otro"));
        tecnologia = catalogo.guardar(new Categoria("Tecnologia"));
        java = catalogo.guardar(new Habilidad("Java"));
        politicaId = politicas.guardar(new PoliticaEstricta()).getId();
    }

    private PublicacionCommand comando(Long categoriaId, long precio, List<LocalDateTime> franjas) {
        return new PublicacionCommand(TipoPublicacion.MENTORIA, "Mentoria Java", "Revision de codigo", categoriaId,
                List.of(java.getId()), Modalidad.REMOTA, 45, 2, precio, Set.of("java"), franjas, politicaId);
    }

    private void cambiar(String userId, Long id, AccionPublicacion accion, String motivo) {
        service.cambiarEstado(userId, new CambiarEstadoCommand(id, accion, motivo));
    }

    @Test
    void creaElBorradorConSusFranjas() {
        Long id = service.crearBorrador("prov", comando(tecnologia.getId(), 800, List.of(INICIO)));
        Publicacion p = publicaciones.buscarPorId(id).orElseThrow();
        assertEquals("BORRADOR", p.getNombreEstado());
        assertEquals(INICIO.plusMinutes(45), p.getFranjas().get(0).getFin());
        assertNotNull(p.getFranjas().get(0).getId());
        assertEquals(List.of(java), p.getHabilidades());
        assertEquals("Estricta", p.getPoliticaCancelacion().getNombre());
        assertSame(tecnologia, p.getCategoria());
    }

    @Test
    void publicarUnaMentoriaValida() {
        Long id = service.crearBorrador("prov", comando(tecnologia.getId(), 800, List.of(INICIO)));
        cambiar("prov", id, AccionPublicacion.PUBLICAR, null);
        Publicacion p = publicaciones.buscarPorId(id).orElseThrow();
        assertEquals("PUBLICADA", p.getNombreEstado());
        assertEquals(AHORA, p.getFechaPublicacion());
    }

    @Test
    void rechazaPublicarUnaOfertaIncompleta() {
        Long id = service.crearBorrador("prov", comando(null, 800, List.of()));
        assertThrows(ReglaNegocioException.class, () -> cambiar("prov", id, AccionPublicacion.PUBLICAR, null));
        assertEquals("BORRADOR", publicaciones.buscarPorId(id).orElseThrow().getNombreEstado());
    }

    @Test
    void precioNegativoSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> service.crearBorrador("prov", comando(tecnologia.getId(), -1, List.of(INICIO))));
    }

    @Test
    void recorreLasAccionesDeEstado() {
        Long id = service.crearBorrador("prov", comando(tecnologia.getId(), 800, List.of(INICIO)));
        cambiar("prov", id, AccionPublicacion.PUBLICAR, null);
        cambiar("prov", id, AccionPublicacion.PAUSAR, null);
        cambiar("prov", id, AccionPublicacion.REANUDAR, null);
        cambiar("prov", id, AccionPublicacion.CERRAR, null);
        assertEquals("CERRADA", publicaciones.buscarPorId(id).orElseThrow().getNombreEstado());
        assertThrows(ReglaNegocioException.class, () -> cambiar("prov", id, AccionPublicacion.REANUDAR, null));

        Long otra = service.crearBorrador("prov", comando(tecnologia.getId(), 800, List.of(INICIO)));
        cambiar("prov", otra, AccionPublicacion.CANCELAR, "Duplicada");
        assertEquals("CANCELADA", publicaciones.buscarPorId(otra).orElseThrow().getNombreEstado());
    }

    @Test
    void soloElAutorModificaLaPublicacion() {
        Long id = service.crearBorrador("prov", comando(tecnologia.getId(), 800, List.of(INICIO)));
        assertThrows(AccesoDenegadoException.class, () -> cambiar("otro", id, AccionPublicacion.PUBLICAR, null));
        assertThrows(AccesoDenegadoException.class,
                () -> service.editar("otro", id, comando(tecnologia.getId(), 1, List.of())));
        assertThrows(RecursoNoEncontradoException.class, () -> cambiar("prov", 99L, AccionPublicacion.PUBLICAR, null));
    }

    @Test
    void editarSoloEnBorrador() {
        Long id = service.crearBorrador("prov", comando(tecnologia.getId(), 800, List.of(INICIO)));
        service.editar("prov", id, comando(tecnologia.getId(), 600, List.of()));
        assertEquals(600, publicaciones.buscarPorId(id).orElseThrow().getPrecioBase().getCantidad());
        cambiar("prov", id, AccionPublicacion.PUBLICAR, null);
        assertThrows(ReglaNegocioException.class,
                () -> service.editar("prov", id, comando(tecnologia.getId(), 1, List.of())));
    }

    @Test
    void validaElAutorYLasReferencias() {
        assertThrows(AccesoDenegadoException.class,
                () -> service.crearBorrador("desconocido", comando(tecnologia.getId(), 1, List.of())));
        Perfil baja = perfiles.guardar(Datos.profesional("baja"));
        baja.darDeBaja();
        assertThrows(AccesoDenegadoException.class,
                () -> service.crearBorrador("baja", comando(tecnologia.getId(), 1, List.of())));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearBorrador("prov", comando(99L, 1, List.of())));
        PublicacionCommand sinPolitica = new PublicacionCommand(TipoPublicacion.MENTORIA, "t", "d", null, null,
                Modalidad.REMOTA, 30, 1, 1, null, null, 99L);
        assertThrows(RecursoNoEncontradoException.class, () -> service.crearBorrador("prov", sinPolitica));
    }

    @Test
    void unProfesionalNoPublicaDesafios() {
        PublicacionCommand desafio = new PublicacionCommand(TipoPublicacion.DESAFIO, "Reto", "d", null, null,
                Modalidad.REMOTA, 30, 1, 1, null, null, null);
        assertThrows(ReglaNegocioException.class, () -> service.crearBorrador("prov", desafio));
    }

    @Test
    void listaTodasLasPropiasConBorradoresYNoLasAjenas() {
        Long borrador = service.crearBorrador("prov", comando(tecnologia.getId(), 800, List.of(INICIO)));
        Long publicada = service.crearBorrador("prov", comando(tecnologia.getId(), 900, List.of(INICIO)));
        cambiar("prov", publicada, AccionPublicacion.PUBLICAR, null);
        service.crearBorrador("otro", comando(tecnologia.getId(), 100, List.of(INICIO)));

        var propias = service.listarPropias("prov");
        assertEquals(List.of(borrador, publicada), propias.stream().map(d -> d.id()).toList());
        assertEquals("BORRADOR", propias.get(0).estado());
        assertEquals("Revision de codigo", propias.get(0).descripcion());
        assertEquals(45, propias.get(0).duracionMinutos());
        assertEquals("Perfil prov", propias.get(0).autor());
        assertEquals(List.of("java"), propias.get(0).etiquetas());
        assertThrows(AccesoDenegadoException.class, () -> service.listarPropias("nadie"));
    }
}
