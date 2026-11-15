package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.PersistenceFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/*
 * Base H2 en memoria en modo MySQL para las pruebas de persistencia.
 * Cada llamada a nueva() crea una base aislada y le aplica las mismas migraciones de Flyway
 * (V1 esquema + V2 datos de demostracion) que se usan con MySQL; Hibernate valida el esquema.
 */
public final class BaseDePrueba {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private BaseDePrueba() {
    }

    public static String urlNueva() {
        return "jdbc:h2:mem:skillswap-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
    }

    /*
     * Con SKILLSWAP_TEST_DB_URL (y SKILLSWAP_TEST_DB_USER / SKILLSWAP_TEST_DB_PASSWORD) las pruebas que crean
     * sus propios datos (ContratacionJpaTest, ConcurrenciaCupoTest) pueden correrse contra un MySQL de prueba.
     */
    public static JpaGestorTransacciones nueva() {
        String url = System.getenv("SKILLSWAP_TEST_DB_URL");
        EntityManagerFactory emf = url == null
                ? PersistenceFactory.crear(urlNueva(), "sa", "")
                : PersistenceFactory.crear(url, System.getenv("SKILLSWAP_TEST_DB_USER"), System.getenv("SKILLSWAP_TEST_DB_PASSWORD"));
        return new JpaGestorTransacciones(emf);
    }

    /* userId unico para que cada prueba trabaje con sus propios datos, aparte de los de demostracion. */
    public static String unico(String prefijo) {
        return prefijo + "-" + SECUENCIA.incrementAndGet();
    }

    /* Ejecuta una consulta de verificacion con un EntityManager nuevo (lee lo que quedo confirmado en la base). */
    public static <T> T leer(JpaGestorTransacciones gestor, Function<EntityManager, T> consulta) {
        try (EntityManager em = gestor.getEntityManagerFactory().createEntityManager()) {
            return consulta.apply(em);
        }
    }
}
