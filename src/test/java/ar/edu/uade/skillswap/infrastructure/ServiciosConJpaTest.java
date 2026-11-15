package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.dto.AccionPublicacion;
import ar.edu.uade.skillswap.application.dto.BilleteraDto;
import ar.edu.uade.skillswap.application.dto.CambiarEstadoCommand;
import ar.edu.uade.skillswap.application.dto.CategoriaDto;
import ar.edu.uade.skillswap.application.dto.CriterioOrden;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.NotificacionDto;
import ar.edu.uade.skillswap.application.dto.PerfilCommand;
import ar.edu.uade.skillswap.application.dto.PerfilPublicoDto;
import ar.edu.uade.skillswap.application.dto.PublicacionCommand;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.application.evento.NotificacionObserver;
import ar.edu.uade.skillswap.application.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.service.BilleteraService;
import ar.edu.uade.skillswap.application.service.BusquedaService;
import ar.edu.uade.skillswap.application.service.NotificacionService;
import ar.edu.uade.skillswap.application.service.PerfilService;
import ar.edu.uade.skillswap.application.service.PublicacionService;
import ar.edu.uade.skillswap.application.service.RecomendacionService;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.Modalidad;
import ar.edu.uade.skillswap.domain.model.TipoEvento;
import ar.edu.uade.skillswap.domain.model.TipoPerfil;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.infrastructure.adapter.CanalNotificacionFactory;
import ar.edu.uade.skillswap.infrastructure.adapter.EmailClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentidadAdapter;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentityClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.PushClientMock;
import ar.edu.uade.skillswap.infrastructure.persistence.BilleteraJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CatalogoJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.ContratacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.NotificacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PoliticaCancelacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PublicacionJpaRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Casos de uso de consulta y de publicacion sobre JPA con los datos de demostracion (V2):
 * los DTO se arman fuera de la transaccion con lo que traen los fetch plans.
 */
class ServiciosConJpaTest {

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final Clock RELOJ = Clock.fixed(LocalDateTime.of(2026, 11, 20, 10, 0).atZone(ZONA).toInstant(), ZONA);

    private static JpaGestorTransacciones gestor;
    private static PerfilJpaRepository perfiles;
    private static PublicacionJpaRepository publicaciones;
    private static ContratacionJpaRepository contrataciones;
    private static CatalogoJpaRepository catalogo;
    private static BilleteraJpaRepository billeteras;
    private static NotificacionJpaRepository notificaciones;
    private static PerfilService perfilService;
    private static PublicacionService publicacionService;
    private static BusquedaService busquedaService;

    @BeforeAll
    static void crearBase() {
        gestor = BaseDePrueba.nueva();
        perfiles = new PerfilJpaRepository(gestor);
        publicaciones = new PublicacionJpaRepository(gestor);
        contrataciones = new ContratacionJpaRepository(gestor);
        catalogo = new CatalogoJpaRepository(gestor);
        billeteras = new BilleteraJpaRepository(gestor);
        notificaciones = new NotificacionJpaRepository(gestor);
        perfilService = new PerfilService(perfiles, publicaciones, contrataciones, catalogo, billeteras, gestor);
        publicacionService = new PublicacionService(publicaciones, perfiles, catalogo,
                new PoliticaCancelacionJpaRepository(gestor), gestor, RELOJ);
        busquedaService = new BusquedaService(publicaciones, catalogo);
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    private static FiltroBusqueda filtro(String texto, Long categoriaId, CriterioOrden orden) {
        return new FiltroBusqueda(texto, categoriaId, null, null, null, null, null, orden, 0, 20);
    }

    @Test
    void elPerfilPublicoDeLaDemoSeArmaFueraDeLaTransaccion() {
        PerfilPublicoDto mateo = perfilService.verPerfilDeUsuario("user-mateo");
        assertEquals("Mateo Silva", mateo.nombreVisible());
        assertEquals(List.of("Java"), mateo.habilidades());
        assertEquals(List.of("Mentoría de Java inicial"),
                mateo.publicacionesActivas().stream().map(PublicacionDto::titulo).toList());
        assertEquals(3, mateo.historialResumido().size());
        assertEquals(5.0, perfilService.verPerfil(6L).reputacion());
    }

    @Test
    void crearPerfilCreaTambienSuBilleteraEnLaMismaTransaccion() {
        String userId = BaseDePrueba.unico("nuevo");
        PerfilPublicoDto dto = perfilService.crearPerfil(userId, new PerfilCommand(TipoPerfil.PROFESIONAL, "Nuevo", null,
                "America/Argentina/Buenos_Aires", List.of(10L), List.of(12L), List.of(), Set.of(CanalNotificacion.EMAIL)));
        assertEquals(List.of("Java"), dto.habilidades());
        assertEquals(0, new BilleteraService(billeteras).consultar(userId).saldo());
        assertThrows(ReglaNegocioException.class, () -> perfilService.crearPerfil(userId,
                new PerfilCommand(TipoPerfil.PROFESIONAL, "Otro", null, null, null, null, null, null)));
    }

    /* Caso de aceptacion 1: publicar una mentoria valida y encontrarla con una busqueda filtrada. */
    @Test
    void publicarUnaMentoriaYEncontrarlaPorCategoriaPadre() {
        Long id = publicacionService.crearBorrador("user-mateo", new PublicacionCommand(TipoPublicacion.MENTORIA,
                "Spring sin Spring", "Inyeccion de dependencias a mano", 2L, List.of(10L), Modalidad.REMOTA, 60, 2,
                150, Set.of("arquitectura"), List.of(LocalDateTime.of(2027, 4, 5, 18, 0)), 1L));
        assertFalse(busquedaService.buscar(filtro("spring sin", 1L, null)).contenido().stream().anyMatch(p -> p.id().equals(id)));

        publicacionService.cambiarEstado("user-mateo", new CambiarEstadoCommand(id, AccionPublicacion.PUBLICAR, null));

        List<PublicacionDto> encontradas = busquedaService.buscar(filtro("spring sin", 1L, null)).contenido();
        assertEquals(List.of(id), encontradas.stream().map(PublicacionDto::id).toList());
        assertEquals("Desarrollo backend", encontradas.get(0).categoria());
        assertEquals(List.of("arquitectura"), encontradas.get(0).etiquetas());
        assertTrue(publicacionService.listarPropias("user-mateo").stream().anyMatch(p -> p.estado().equals("BORRADOR")));
    }

    /* Caso de aceptacion 2: una oferta incompleta no se publica y queda en BORRADOR. */
    @Test
    void unaOfertaIncompletaNoSePublica() {
        Long id = publicacionService.crearBorrador("user-mateo", new PublicacionCommand(TipoPublicacion.MENTORIA,
                "Sin franjas", null, null, List.of(), Modalidad.REMOTA, 60, 1, 100, Set.of(), List.of(), null));
        assertThrows(ReglaNegocioException.class, () -> publicacionService.cambiarEstado("user-mateo",
                new CambiarEstadoCommand(id, AccionPublicacion.PUBLICAR, null)));
        assertEquals("BORRADOR", publicaciones.buscarPorId(id).orElseThrow().getNombreEstado());
    }

    @Test
    void laBusquedaDeLaDemoUsaElCompositeYNoHaceNMasUno() {
        Statistics estadisticas = gestor.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        List<PublicacionDto> tecnologia = busquedaService.buscar(filtro(null, 1L, CriterioOrden.MENOR_PRECIO)).contenido();
        long consultas = estadisticas.getPrepareStatementCount();
        estadisticas.setStatisticsEnabled(false);

        assertTrue(tecnologia.stream().map(PublicacionDto::titulo).toList().containsAll(List.of(
                "Preparación para entrevistas técnicas", "Arquitectura limpia aplicada", "Mentoría de Java inicial",
                "Desafío: prototipo de app de turnos")));
        assertTrue(tecnologia.stream().noneMatch(p -> p.estado().equals("BORRADOR")));
        // catalogo (1) + COUNT (1) + pagina con JOIN FETCH (1) + etiquetas y habilidades de a lotes (2)
        assertTrue(consultas <= 5, "consultas: " + consultas);
        List<String> categorias = busquedaService.listarCategorias().stream().map(CategoriaDto::nombre).toList();
        assertTrue(categorias.containsAll(List.of("Tecnología", "Diseño", "Creatividad", "Negocios", "Productividad")));
    }

    @Test
    void laRecomendacionPorAfinidadUsaLosInteresesPersistidos() {
        RecomendacionService recomendacion = new RecomendacionService(new AfinidadPorHabilidades(), perfiles, publicaciones);
        List<String> titulos = recomendacion.recomendar("user-mateo", 0, 10).contenido().stream()
                .map(PublicacionDto::titulo).toList();
        assertTrue(titulos.containsAll(List.of("Arquitectura limpia aplicada", "Revisión profesional de portfolio")));
        assertFalse(titulos.contains("Mentoría de Java inicial"), "no recomienda publicaciones propias");
    }

    @Test
    void billeteraYBandejaDeLaDemo() {
        BilleteraService billetera = new BilleteraService(billeteras);
        String userId = BaseDePrueba.unico("billetera");
        perfilService.crearPerfil(userId, new PerfilCommand(TipoPerfil.PROFESIONAL, "Con saldo", null, null,
                null, null, null, null));
        BilleteraDto acreditada = billetera.acreditarDemo(userId, 300);
        assertEquals(300, acreditada.saldo());
        assertEquals(1, acreditada.movimientos().size());
        assertEquals(300, billetera.consultar(userId).saldo());
        assertEquals(2160, billetera.consultar("user-mateo").saldo());
        assertEquals(3, billetera.consultar("user-mateo").movimientos().size());

        NotificacionService bandeja = new NotificacionService(notificaciones);
        var pagina = bandeja.listar("user-mateo", 0, 2);
        assertEquals(4, pagina.totalElementos());
        assertEquals(List.of("SOLICITUD_RECIBIDA", "SESION_COMPLETADA"), pagina.contenido().stream().map(NotificacionDto::tipo).toList());
        Long noLeida = pagina.contenido().get(0).id();
        bandeja.marcarLeida("user-mateo", noLeida);
        assertTrue(notificaciones.buscarPorId(noLeida).orElseThrow().isLeida());
    }

    /* Caso de aceptacion 8: un mismo evento llega por email y por la bandeja interna (adaptadores mock). */
    @Test
    void unEventoSeNotificaPorDosCanales() {
        EmailClientMock email = new EmailClientMock();
        IdentidadAdapter identidad = new IdentidadAdapter(new IdentityClientMock());
        NotificacionObserver observer = new NotificacionObserver(perfiles,
                new CanalNotificacionFactory(email, new PushClientMock(), identidad, notificaciones));
        long antes = notificaciones.listarPorDestinatario("user-laura", 0, 1).totalElementos();

        // user-laura eligio INTERNA y EMAIL en los datos de demostracion
        observer.actualizar(new EventoContratacion(TipoEvento.SOLICITUD_RECIBIDA, 99L, "user-mateo", "user-laura",
                LocalDateTime.of(2026, 11, 20, 10, 0)));

        assertEquals(1, email.getEnviados().size());
        assertEquals("laura.rossi@skillswap.test", email.getEnviados().get(0).to());
        assertEquals(antes + 1, notificaciones.listarPorDestinatario("user-laura", 0, 1).totalElementos());
    }
}
