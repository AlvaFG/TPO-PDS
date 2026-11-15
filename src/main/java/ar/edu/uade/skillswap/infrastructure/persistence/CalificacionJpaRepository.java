package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.port.ICalificacionRepository;
import ar.edu.uade.skillswap.domain.model.Calificacion;

import java.util.List;

/*
 * Repositorio JPA de calificaciones. UNIQUE(contratacion_id, autor_id) en la base.
 * Fetch plan: autor y calificado con JOIN FETCH (Perfil.recalcularReputacion lee el calificado).
 */
public class CalificacionJpaRepository implements ICalificacionRepository {

    private final JpaGestorTransacciones gestor;

    public CalificacionJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Calificacion guardar(Calificacion calificacion) {
        return gestor.ejecutar(() -> {
            if (calificacion.getId() == null) {
                gestor.entityManager().persist(calificacion);
                return calificacion;
            }
            return gestor.entityManager().merge(calificacion);
        });
    }

    @Override
    public List<Calificacion> listarRecibidas(Long perfilId) {
        return gestor.ejecutar(() -> gestor.entityManager()
                .createQuery("select c from Calificacion c join fetch c.autor join fetch c.calificado k "
                        + "where k.id = :perfilId order by c.id", Calificacion.class)
                .setParameter("perfilId", perfilId)
                .getResultList());
    }
}
