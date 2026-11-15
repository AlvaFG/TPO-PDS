package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.PersistenceFactory;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/* Arranque: Flyway aplica V1 y V2 y Hibernate valida el mapeo contra el esquema (hbm2ddl=validate). */
class ArranquePersistenciaTest {

    @Test
    void flywayMigraYHibernateValidaElEsquema() {
        String url = BaseDePrueba.urlNueva();
        EntityManagerFactory emf = PersistenceFactory.crear(url, "sa", "");
        try {
            assertTrue(emf.isOpen());
            assertEquals("validate", emf.getProperties().get("hibernate.hbm2ddl.auto"));
            var migraciones = Flyway.configure().dataSource(url, "sa", "").load().info().applied();
            assertEquals(2, migraciones.length);
            assertEquals("1", migraciones[0].getVersion().getVersion());
            assertEquals("2", migraciones[1].getVersion().getVersion());
        } finally {
            emf.close();
        }
    }

    @Test
    void migrarDosVecesNoRepiteLasMigraciones() {
        String url = BaseDePrueba.urlNueva();
        PersistenceFactory.migrar(url, "sa", "");
        PersistenceFactory.migrar(url, "sa", "");
        assertEquals(2, Flyway.configure().dataSource(url, "sa", "").load().info().applied().length);
    }

    @Test
    void losDatosDeDemostracionQuedanCargados() {
        JpaGestorTransacciones gestor = BaseDePrueba.nueva();
        long perfiles = BaseDePrueba.leer(gestor, em -> em.createQuery("select count(p) from Perfil p", Long.class).getSingleResult());
        long saldoMateo = BaseDePrueba.leer(gestor, em -> em.createQuery(
                "select b.saldo.cantidad from Billetera b where b.titular.userId = 'user-mateo'", Long.class).getSingleResult());
        assertEquals(8, perfiles);
        assertEquals(2160, saldoMateo);
        gestor.getEntityManagerFactory().close();
    }
}
