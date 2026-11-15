package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.port.ICatalogoRepository;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/*
 * Repositorio JPA del catalogo (Composite en SINGLE_TABLE).
 * Fetch plan: el catalogo es chico y cambia poco, asi que se trae entero en una consulta
 * (todas las categorias con sus hijos). El arbol queda armado en memoria y el Composite
 * (listarSubcategoriaIds, contiene) se recorre sin consultas recursivas ni N+1.
 */
public class CatalogoJpaRepository implements ICatalogoRepository {

    private final JpaGestorTransacciones gestor;

    public CatalogoJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Optional<Categoria> buscarCategoria(Long id) {
        return gestor.ejecutar(() -> {
            cargarArbol(gestor.entityManager());
            return Optional.ofNullable(gestor.entityManager().find(Categoria.class, id));
        });
    }

    @Override
    public List<Habilidad> buscarHabilidades(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return gestor.ejecutar(() -> gestor.entityManager()
                .createQuery("select h from Habilidad h where h.id in :ids order by h.id", Habilidad.class)
                .setParameter("ids", ids)
                .getResultList());
    }

    @Override
    public List<Categoria> listarCategorias() {
        return gestor.ejecutar(() -> cargarArbol(gestor.entityManager()));
    }

    private static List<Categoria> cargarArbol(EntityManager em) {
        return em.createQuery("select distinct c from Categoria c left join fetch c.hijos order by c.id", Categoria.class)
                .getResultList();
    }
}
