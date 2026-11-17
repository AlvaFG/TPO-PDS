package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.port.IContratacionRepository;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * Repositorio JPA de contrataciones.
 * Fetch plan: publicacion, cliente, proveedor y franja con JOIN FETCH. La franja se trae como entidad
 * real (no proxy) para que publicacion.getFranjas().contains(franja) compare la misma instancia.
 */
public class ContratacionJpaRepository implements IContratacionRepository {

    private static final String CON_PARTES = "select c from Contratacion c join fetch c.publicacion "
            + "join fetch c.cliente cl join fetch c.proveedor pr join fetch c.franja ";

    private final JpaGestorTransacciones gestor;

    public ContratacionJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Contratacion guardar(Contratacion contratacion) {
        return gestor.ejecutar(() -> {
            if (contratacion.getId() == null) {
                gestor.entityManager().persist(contratacion);
                return contratacion;
            }
            return gestor.entityManager().merge(contratacion);
        });
    }

    @Override
    public Optional<Contratacion> buscarPorId(Long id) {
        return gestor.ejecutar(() -> {
            Optional<Contratacion> contratacion = gestor.entityManager()
                    .createQuery(CON_PARTES + "left join fetch c.propuesta.nuevaFranja where c.id = :id", Contratacion.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
            contratacion.ifPresent(c -> c.getHistorial().size());
            return contratacion;
        });
    }

    /* Paginado en la consulta; ContratacionDto lee publicacion, franja y la contraparte (ya traidas). */
    @Override
    public Pagina<Contratacion> listarPorParticipante(String userId, int pagina, int tamanio) {
        return gestor.ejecutar(() -> {
            EntityManager em = gestor.entityManager();
            long total = em.createQuery("select count(c) from Contratacion c "
                            + "where c.cliente.userId = :userId or c.proveedor.userId = :userId", Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
            List<Contratacion> contenido = em.createQuery(CON_PARTES
                            + "left join fetch c.propuesta.nuevaFranja "
                            + "where cl.userId = :userId or pr.userId = :userId order by c.id desc", Contratacion.class)
                    .setParameter("userId", userId)
                    .setFirstResult(pagina * tamanio)
                    .setMaxResults(tamanio)
                    .getResultList();
            return new Pagina<>(contenido, pagina, tamanio, total);
        });
    }

    /* Agregacion en la base: la fecha de la solicitud es la primera entrada del historial (orden 0). */
    @Override
    public Map<Long, Long> contarRecientesPorPublicacion(LocalDateTime desde) {
        return gestor.ejecutar(() -> gestor.entityManager()
                .createQuery("select c.publicacion.id, count(c) from Contratacion c join c.historial h "
                        + "where index(h) = 0 and h.fecha >= :desde group by c.publicacion.id", Object[].class)
                .setParameter("desde", desde)
                .getResultStream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1])));
    }
}
