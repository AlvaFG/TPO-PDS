package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.dto.CriterioOrden;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.infrastructure.persistence.ContratacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PersistenceFactory;
import ar.edu.uade.skillswap.infrastructure.persistence.PublicacionJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Arranque contra un MySQL real (docker-compose.yml). Solo corre si se define SKILLSWAP_DB_URL, por ejemplo:
 *   docker compose up -d
 *   SKILLSWAP_DB_URL='jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC' mvn test -Dtest=MySqlArranqueTest
 * Solo lee: no modifica los datos de demostracion.
 */
@EnabledIfEnvironmentVariable(named = "SKILLSWAP_DB_URL", matches = ".+")
class MySqlArranqueTest {

    @Test
    void flywayYValidateContraMySqlYLecturaDeLaDemo() {
        EntityManagerFactory emf = PersistenceFactory.crear();
        try {
            JpaGestorTransacciones gestor = new JpaGestorTransacciones(emf);
            var mateo = new PerfilJpaRepository(gestor).buscarPorUserId("user-mateo").orElseThrow();
            assertEquals("Mateo Silva", mateo.getNombreVisible());

            var pagina = new PublicacionJpaRepository(gestor).buscar(new FiltroBusqueda("java", null, null, null, null,
                    null, null, CriterioOrden.MENOR_PRECIO, 0, 10), List.of());
            assertTrue(pagina.totalElementos() >= 2);

            var contrataciones = new ContratacionJpaRepository(gestor).listarPorParticipante("user-mateo", 0, 10);
            assertTrue(contrataciones.totalElementos() >= 3);
        } finally {
            emf.close();
        }
    }
}
