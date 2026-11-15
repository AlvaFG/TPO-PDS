package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.soporte.Datos;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/* Commit, rollback y EntityManager compartido del gestor de transacciones JPA. */
class JpaGestorTransaccionesTest {

    private static JpaGestorTransacciones gestor;
    private static PerfilJpaRepository perfiles;

    @BeforeAll
    static void crearBase() {
        gestor = BaseDePrueba.nueva();
        perfiles = new PerfilJpaRepository(gestor);
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    @Test
    void confirmaLoQueHaceLaOperacion() {
        String userId = BaseDePrueba.unico("commit");
        gestor.ejecutar(() -> perfiles.guardar(Datos.profesional(userId)));
        assertTrue(perfiles.buscarPorUserId(userId).isPresent());
    }

    @Test
    void siLaOperacionFallaHaceRollbackYRelanzaLaMismaExcepcion() {
        String userId = BaseDePrueba.unico("rollback");
        ReglaNegocioException error = new ReglaNegocioException("falla simulada");
        ReglaNegocioException lanzada = assertThrows(ReglaNegocioException.class, () -> gestor.ejecutar(() -> {
            perfiles.guardar(Datos.profesional(userId));
            gestor.entityManager().flush(); // el INSERT ya llego a la base dentro de la transaccion
            throw error;
        }));
        assertSame(error, lanzada);
        assertTrue(perfiles.buscarPorUserId(userId).isEmpty());
    }

    @Test
    void losRepositoriosDentroDeLaOperacionCompartenElEntityManager() {
        String userId = BaseDePrueba.unico("compartido");
        boolean mismaInstancia = gestor.ejecutar(() -> {
            EntityManager em = gestor.entityManager();
            Perfil guardado = perfiles.guardar(Datos.profesional(userId));
            Perfil leido = perfiles.buscarPorUserId(userId).orElseThrow();
            return guardado == leido && em == gestor.entityManager() && em.contains(leido);
        });
        assertTrue(mismaInstancia);
    }

    @Test
    void sinTransaccionEnCursoCadaRepositorioUsaSuPropioEntityManager() {
        String userId = BaseDePrueba.unico("suelto");
        Perfil guardado = perfiles.guardar(Datos.profesional(userId));
        assertNotNull(guardado.getId());
        assertThrows(IllegalStateException.class, () -> gestor.entityManager());
        assertEquals(userId, perfiles.buscarPorId(guardado.getId()).orElseThrow().getUserId());
    }
}
