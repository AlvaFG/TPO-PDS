package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.port.IPoliticaCancelacionRepository;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;

import java.util.Optional;

/* Repositorio JPA de politicas de cancelacion (Strategy en SINGLE_TABLE: devuelve la subclase concreta). */
public class PoliticaCancelacionJpaRepository implements IPoliticaCancelacionRepository {

    private final JpaGestorTransacciones gestor;

    public PoliticaCancelacionJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Optional<PoliticaCancelacion> buscarPorId(Long id) {
        return gestor.ejecutar(() -> Optional.ofNullable(gestor.entityManager().find(PoliticaCancelacion.class, id)));
    }
}
