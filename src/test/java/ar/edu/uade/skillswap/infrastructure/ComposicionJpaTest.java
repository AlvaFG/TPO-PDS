package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.ConfiguracionApp;
import ar.edu.uade.skillswap.controllers.UiControllers;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Integracion controllers + casos de uso + persistencia: el mismo cableado que hace el Composition Root
 * con los repositorios JPA y los adaptadores mock, sobre H2 con los datos de demostracion.
 */
class ComposicionJpaTest {

    private static JpaGestorTransacciones gestor;
    private static UiControllers controllers;

    @BeforeAll
    static void armar() {
        gestor = BaseDePrueba.nueva();
        IdentidadAdapter identidad = new IdentidadAdapter(new IdentityClientMock());
        NotificacionJpaRepository notificaciones = new NotificacionJpaRepository(gestor);
        controllers = ConfiguracionApp.crear(new ConfiguracionApp.Puertos(
                new PerfilJpaRepository(gestor), new PublicacionJpaRepository(gestor),
                new ContratacionJpaRepository(gestor), new BilleteraJpaRepository(gestor), notificaciones,
                new CalificacionJpaRepository(gestor), new CatalogoJpaRepository(gestor),
                new PoliticaCancelacionJpaRepository(gestor), gestor, identidad,
                new CanalNotificacionFactory(new EmailClientMock(), new PushClientMock(), identidad, notificaciones),
                Clock.systemDefaultZone()));
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    @Test
    void laUiConLosControllersRealesLeeLosDatosDeDemostracion() {
        assertTrue(controllers.busqueda().categorias().contains("Tecnología"));
        assertEquals(2160, controllers.wallet().consultarSaldo("user-mateo"));
        assertEquals(3, controllers.contrataciones().listarContrataciones("user-mateo").size());
        assertFalse(controllers.publicaciones().listarPropias("user-mateo").isEmpty());
        assertFalse(controllers.notificaciones().listarNotificaciones("user-mateo").isEmpty());
        assertEquals("Mateo Silva", controllers.perfiles().verPerfil("user-mateo").nombre());
    }
}
