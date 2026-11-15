package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import ar.edu.uade.skillswap.application.port.IGestorTransacciones;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;

import java.util.function.Supplier;

/*
 * Limite transaccional de los casos de uso con JPA (RESOURCE_LOCAL).
 * Abre un EntityManager y una transaccion por operacion y lo deja en el hilo actual: todos los
 * repositorios que se usen dentro de la operacion comparten ese EntityManager.
 * Commit si la operacion termina; rollback y se relanza la misma excepcion si falla.
 * El conflicto de @Version (OptimisticLockException al hacer flush) se traduce a
 * ConflictoConcurrenciaException, que es lo unico de JPA que no debe llegar a las capas de arriba.
 * Si ya hay una transaccion en curso en el hilo, la operacion se une a ella.
 */
public class JpaGestorTransacciones implements IGestorTransacciones {

    private final EntityManagerFactory emf;
    private final ThreadLocal<EntityManager> actual = new ThreadLocal<>();

    public JpaGestorTransacciones(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public <T> T ejecutar(Supplier<T> operacion) {
        if (actual.get() != null) {
            return operacion.get();
        }
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        actual.set(em);
        try {
            tx.begin();
            T resultado = operacion.get();
            em.flush(); // los conflictos de version salen aca como OptimisticLockException
            tx.commit();
            return resultado;
        } catch (OptimisticLockException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new ConflictoConcurrenciaException(
                    "La publicacion o el cupo fueron modificados por otra operacion. Reintente.", e);
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            actual.remove();
            em.close();
        }
    }

    /* EntityManager de la transaccion en curso. Los repositorios lo piden dentro de ejecutar(...). */
    public EntityManager entityManager() {
        EntityManager em = actual.get();
        if (em == null) {
            throw new IllegalStateException("No hay una transaccion en curso");
        }
        return em;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}
