package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.port.IBilleteraRepository;
import ar.edu.uade.skillswap.domain.model.Billetera;

import java.util.Optional;

/*
 * Repositorio JPA de billeteras.
 * Fetch plan: titular y movimientos con JOIN FETCH en una sola consulta (BilleteraDto y yaPago los leen).
 */
public class BilleteraJpaRepository implements IBilleteraRepository {

    private final JpaGestorTransacciones gestor;

    public BilleteraJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Billetera guardar(Billetera billetera) {
        return gestor.ejecutar(() -> {
            if (billetera.getId() == null) {
                gestor.entityManager().persist(billetera);
                return billetera;
            }
            return gestor.entityManager().merge(billetera);
        });
    }

    @Override
    public Optional<Billetera> buscarPorUserId(String userId) {
        return gestor.ejecutar(() -> gestor.entityManager()
                .createQuery("select b from Billetera b join fetch b.titular t left join fetch b.movimientos "
                        + "where t.userId = :userId", Billetera.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst());
    }
}
