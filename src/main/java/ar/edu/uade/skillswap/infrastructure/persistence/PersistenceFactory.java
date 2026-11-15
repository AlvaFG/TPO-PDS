package ar.edu.uade.skillswap.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;

import java.util.HashMap;
import java.util.Map;

/*
 * Arranque de la persistencia: corre las migraciones de Flyway y despues crea el EntityManagerFactory
 * de la unidad "skillswap" (persistence.xml), que solo valida el esquema.
 * Por defecto usa el MySQL de docker-compose.yml; las variables de entorno SKILLSWAP_DB_URL,
 * SKILLSWAP_DB_USER y SKILLSWAP_DB_PASSWORD permiten apuntar a otra base.
 */
public final class PersistenceFactory {

    public static final String UNIDAD = "skillswap";
    public static final String URL_MYSQL = "jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC";
    public static final String USUARIO_MYSQL = "skillswap";
    public static final String CLAVE_MYSQL = "skillswap";

    private PersistenceFactory() {
    }

    /* MySQL local (o el indicado por las variables de entorno). */
    public static EntityManagerFactory crear() {
        return crear(System.getenv().getOrDefault("SKILLSWAP_DB_URL", URL_MYSQL),
                System.getenv().getOrDefault("SKILLSWAP_DB_USER", USUARIO_MYSQL),
                System.getenv().getOrDefault("SKILLSWAP_DB_PASSWORD", CLAVE_MYSQL));
    }

    /* Migra la base indicada y abre la unidad de persistencia contra ella (MySQL o H2 en modo MySQL). */
    public static EntityManagerFactory crear(String url, String usuario, String clave) {
        migrar(url, usuario, clave);
        return abrir(url, usuario, clave);
    }

    /* Aplica las migraciones pendientes de db/migration (V1 esquema, V2 datos de demostracion). */
    public static void migrar(String url, String usuario, String clave) {
        Flyway.configure()
                .dataSource(url, usuario, clave)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    /* Crea el EntityManagerFactory sin migrar (el esquema ya tiene que existir: hbm2ddl=validate). */
    public static EntityManagerFactory abrir(String url, String usuario, String clave) {
        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("jakarta.persistence.jdbc.url", url);
        propiedades.put("jakarta.persistence.jdbc.user", usuario);
        propiedades.put("jakarta.persistence.jdbc.password", clave);
        propiedades.put("jakarta.persistence.jdbc.driver", url.startsWith("jdbc:h2:") ? "org.h2.Driver" : "com.mysql.cj.jdbc.Driver");
        return Persistence.createEntityManagerFactory(UNIDAD, propiedades);
    }
}
