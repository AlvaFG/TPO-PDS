package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.domain.model.Perfil;

import java.util.Optional;

/*
 * Repositorio JPA de perfiles. Cada metodo corre en la transaccion en curso o abre la suya.
 * Fetch plan: canales con JOIN FETCH; habilidades, intereses y disponibilidad se inicializan dentro
 * del EntityManager (son listas: Hibernate no hace JOIN FETCH de dos listas a la vez). Son 4 consultas
 * fijas y el perfil queda listo para usarse fuera de la transaccion (observers, perfil publico, recomendacion).
 */
public class PerfilJpaRepository implements IPerfilRepository {

    private final JpaGestorTransacciones gestor;

    public PerfilJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Perfil guardar(Perfil perfil) {
        return gestor.ejecutar(() -> {
            if (perfil.getId() == null) {
                gestor.entityManager().persist(perfil);
                return perfil;
            }
            return gestor.entityManager().merge(perfil);
        });
    }

    @Override
    public Optional<Perfil> buscarPorId(Long id) {
        return buscarUno("p.id = :valor", id);
    }

    @Override
    public Optional<Perfil> buscarPorUserId(String userId) {
        return buscarUno("p.userId = :valor", userId);
    }

    private Optional<Perfil> buscarUno(String condicion, Object valor) {
        return gestor.ejecutar(() -> {
            Optional<Perfil> perfil = gestor.entityManager()
                    .createQuery("select p from Perfil p left join fetch p.canales where " + condicion, Perfil.class)
                    .setParameter("valor", valor)
                    .getResultStream()
                    .findFirst();
            perfil.ifPresent(PerfilJpaRepository::inicializar);
            return perfil;
        });
    }

    /* Carga las listas LAZY mientras el EntityManager esta abierto. */
    static void inicializar(Perfil perfil) {
        perfil.getHabilidades().size();
        perfil.getIntereses().size();
        perfil.getDisponibilidad().size();
    }
}
