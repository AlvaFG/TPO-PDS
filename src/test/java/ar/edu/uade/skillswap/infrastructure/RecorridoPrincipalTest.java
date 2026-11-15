package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.ConfiguracionApp;
import ar.edu.uade.skillswap.application.dto.CalificarCommand;
import ar.edu.uade.skillswap.application.dto.PerfilCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.evento.ReputacionObserver;
import ar.edu.uade.skillswap.application.service.CalificacionService;
import ar.edu.uade.skillswap.application.service.PerfilService;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.TipoPerfil;
import ar.edu.uade.skillswap.infrastructure.adapter.CanalNotificacionFactory;
import ar.edu.uade.skillswap.infrastructure.adapter.EmailClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentidadAdapter;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentityClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.PushClientMock;
import ar.edu.uade.skillswap.infrastructure.persistence.BilleteraJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CalificacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CatalogoJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.ContratacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.NotificacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PoliticaCancelacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PublicacionJpaRepository;
import ar.edu.uade.skillswap.model.Models.Contratacion;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Recorrido principal de la consigna (seccion 13), de extremo a extremo y sobre JPA:
 * crear perfiles -> publicar -> buscar -> solicitar -> aceptar -> pagar -> iniciar -> completar -> calificar.
 * Se usan los controllers de la UI (controllers -> casos de uso -> repositorios JPA -> base con migraciones).
 * La UI del kit no expone crear perfil ni calificar: esos dos pasos llaman a los servicios de application.
 */
class RecorridoPrincipalTest {

    private static final String PROVEEDOR = "user-e2e-proveedor";
    private static final String CLIENTE = "user-e2e-cliente";

    private static JpaGestorTransacciones gestor;
    private static UiControllers ui;
    private static PerfilService perfiles;
    private static CalificacionService calificaciones;

    @BeforeAll
    static void armar() {
        gestor = BaseDePrueba.nueva();
        Map<String, String> usuarios = new HashMap<>(IdentityClientMock.USUARIOS_DEMO);
        usuarios.put(PROVEEDOR, "proveedor.e2e@skillswap.test");
        usuarios.put(CLIENTE, "cliente.e2e@skillswap.test");
        IdentidadAdapter identidad = new IdentidadAdapter(new IdentityClientMock(usuarios));
        NotificacionJpaRepository notificaciones = new NotificacionJpaRepository(gestor);
        PerfilJpaRepository perfilRepo = new PerfilJpaRepository(gestor);
        PublicacionJpaRepository publicacionRepo = new PublicacionJpaRepository(gestor);
        ContratacionJpaRepository contratacionRepo = new ContratacionJpaRepository(gestor);
        BilleteraJpaRepository billeteraRepo = new BilleteraJpaRepository(gestor);
        CalificacionJpaRepository calificacionRepo = new CalificacionJpaRepository(gestor);
        CatalogoJpaRepository catalogo = new CatalogoJpaRepository(gestor);
        ui = ConfiguracionApp.crear(new ConfiguracionApp.Puertos(perfilRepo, publicacionRepo, contratacionRepo,
                billeteraRepo, notificaciones, calificacionRepo, catalogo,
                new PoliticaCancelacionJpaRepository(gestor), gestor, identidad,
                new CanalNotificacionFactory(new EmailClientMock(), new PushClientMock(), identidad, notificaciones),
                Clock.systemDefaultZone()));
        perfiles = new PerfilService(perfilRepo, publicacionRepo, contratacionRepo, catalogo, billeteraRepo, gestor);
        PublicadorEventos eventos = new PublicadorEventos();
        eventos.suscribir(new ReputacionObserver(perfilRepo, calificacionRepo));
        calificaciones = new CalificacionService(contratacionRepo, calificacionRepo, gestor, eventos);
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    private static void crearPerfil(String userId, String nombre) {
        perfiles.crearPerfil(userId, new PerfilCommand(TipoPerfil.PROFESIONAL, nombre, "Perfil de prueba",
                "America/Argentina/Buenos_Aires", List.of(10L), List.of(), List.of(), Set.of(CanalNotificacion.INTERNA)));
    }

    @Test
    void publicarSolicitarAceptarPagarCompletarYCalificar() {
        crearPerfil(PROVEEDOR, "Proveedor E2E");
        crearPerfil(CLIENTE, "Cliente E2E");

        // publicar y encontrarla con una busqueda filtrada
        Publicacion borrador = ui.publicaciones().crearBorrador(PROVEEDOR, "Mentoria E2E de Java",
                "Recorrido principal", "Desarrollo backend", Modalidad.REMOTA, 60, 800);
        assertEquals(EstadoPublicacion.BORRADOR, borrador.estado());
        Publicacion publicada = ui.publicaciones().cambiarEstado(PROVEEDOR, borrador.id(), EstadoPublicacion.PUBLICADA);
        assertEquals(EstadoPublicacion.PUBLICADA, publicada.estado());
        List<Publicacion> encontradas = ui.busqueda()
                .buscar(new FiltrosBusqueda("E2E", "Desarrollo backend", Modalidad.REMOTA, 1000, "Relevancia"));
        assertEquals(List.of(publicada.id()), encontradas.stream().map(Publicacion::id).toList());

        // solicitar -> aceptar -> pagar con saldo suficiente
        assertEquals(1000, ui.wallet().acreditarDemo(CLIENTE, 1000));
        Contratacion c = ui.contrataciones().solicitar(CLIENTE, publicada.id(), "Aprender Java");
        assertEquals(EstadoContratacion.PENDIENTE, c.estado());
        assertEquals(EstadoContratacion.ACEPTADA, ui.contrataciones().avanzar(PROVEEDOR, c.id()).estado());
        assertEquals(EstadoContratacion.CONFIRMADA, ui.contrataciones().avanzar(CLIENTE, c.id()).estado());
        assertEquals(200, ui.wallet().consultarSaldo(CLIENTE));

        // iniciar y completar: el proveedor cobra
        assertEquals(EstadoContratacion.EN_CURSO, ui.contrataciones().avanzar(PROVEEDOR, c.id()).estado());
        assertEquals(EstadoContratacion.COMPLETADA, ui.contrataciones().avanzar(PROVEEDOR, c.id()).estado());
        assertEquals(800, ui.wallet().consultarSaldo(PROVEEDOR));

        // calificar (servicio de application): una sola vez y la reputacion se recalcula
        Long contratacionId = Long.valueOf(c.id());
        calificaciones.calificar(CLIENTE, new CalificarCommand(contratacionId, 5, "Excelente"));
        assertEquals(5.0, ui.perfiles().verPerfil(PROVEEDOR).reputacion());
        assertThrows(ReglaNegocioException.class,
                () -> calificaciones.calificar(CLIENTE, new CalificarCommand(contratacionId, 1, "Otra vez")));
        assertEquals(5.0, ui.perfiles().verPerfil(PROVEEDOR).reputacion());

        // el historial y la bandeja reflejan el recorrido
        assertEquals(EstadoContratacion.COMPLETADA, ui.contrataciones().listarContrataciones(CLIENTE).get(0).estado());
        assertFalse(ui.notificaciones().listarNotificaciones(PROVEEDOR).isEmpty());
    }
}
