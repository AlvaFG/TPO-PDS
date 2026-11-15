package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.port.INotificacionRepository;
import ar.edu.uade.skillswap.domain.model.Notificacion;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/* Repositorio JPA de la bandeja interna. Indice (destinatario_id, leida). */
public class NotificacionJpaRepository implements INotificacionRepository {

    private final JpaGestorTransacciones gestor;

    public NotificacionJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        return gestor.ejecutar(() -> {
            if (notificacion.getId() == null) {
                gestor.entityManager().persist(notificacion);
                return notificacion;
            }
            return gestor.entityManager().merge(notificacion);
        });
    }

    @Override
    public Optional<Notificacion> buscarPorId(Long id) {
        return gestor.ejecutar(() -> Optional.ofNullable(gestor.entityManager().find(Notificacion.class, id)));
    }

    @Override
    public Pagina<Notificacion> listarPorDestinatario(String userId, int pagina, int tamanio) {
        return gestor.ejecutar(() -> {
            EntityManager em = gestor.entityManager();
            long total = em.createQuery("select count(n) from Notificacion n where n.destinatarioId = :userId", Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            List<Notificacion> contenido = em.createQuery("select n from Notificacion n "
                            + "where n.destinatarioId = :userId order by n.fecha desc, n.id desc", Notificacion.class)
                    .setParameter("userId", userId)
                    .setFirstResult(pagina * tamanio)
                    .setMaxResults(tamanio)
                    .getResultList();
            return new Pagina<>(contenido, pagina, tamanio, total);
        });
    }
}
