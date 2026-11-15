package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.dto.CriterioOrden;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPausada;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicada;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/*
 * Repositorio JPA de publicaciones.
 * Fetch plan de los listados: autor y categoria con JOIN FETCH (a uno, no rompen la paginacion);
 * etiquetas y habilidades se cargan de a lotes (hibernate.default_batch_fetch_size), una consulta por
 * coleccion para toda la pagina. Asi PublicacionDto se arma sin N+1 y fuera de la transaccion.
 */
public class PublicacionJpaRepository implements IPublicacionRepository {

    private static final String LISTADO = "select p from Publicacion p join fetch p.autor a left join fetch p.categoria ";

    private final JpaGestorTransacciones gestor;

    public PublicacionJpaRepository(JpaGestorTransacciones gestor) {
        this.gestor = gestor;
    }

    @Override
    public Publicacion guardar(Publicacion publicacion) {
        return gestor.ejecutar(() -> {
            if (publicacion.getId() == null) {
                gestor.entityManager().persist(publicacion);
                return publicacion;
            }
            return gestor.entityManager().merge(publicacion);
        });
    }

    /* Agregado completo: autor, categoria y politica con JOIN FETCH; las colecciones, de a lotes. */
    @Override
    public Optional<Publicacion> buscarPorId(Long id) {
        return gestor.ejecutar(() -> {
            Optional<Publicacion> publicacion = gestor.entityManager()
                    .createQuery("select p from Publicacion p join fetch p.autor left join fetch p.categoria "
                            + "left join fetch p.politicaCancelacion where p.id = :id", Publicacion.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
            publicacion.ifPresent(p -> {
                p.getFranjas().size();
                p.getPromociones().size();
                p.getHistorial().size();
                inicializarListado(p);
            });
            return publicacion;
        });
    }

    /*
     * Filtros, orden y paginacion en la consulta (WHERE, ORDER BY y LIMIT/OFFSET), con un COUNT aparte.
     * Usa los indices de V1: (nombre_estado, categoria_id), precio_base y fecha_publicacion.
     */
    @Override
    public Pagina<Publicacion> buscar(FiltroBusqueda filtro, List<Long> categoriaIds) {
        Map<String, Object> parametros = new HashMap<>();
        StringBuilder where = new StringBuilder(" where p.nombreEstado = :estado");
        parametros.put("estado", EstadoPublicada.NOMBRE);
        String texto = filtro.texto() == null || filtro.texto().isBlank()
                ? null : "%" + filtro.texto().trim().toLowerCase(Locale.ROOT) + "%";
        if (texto != null) {
            where.append(" and (lower(p.titulo) like :texto or lower(p.descripcion) like :texto"
                    + " or exists (select 1 from p.etiquetas e where lower(e) like :texto))");
            parametros.put("texto", texto);
        }
        if (categoriaIds != null && !categoriaIds.isEmpty()) {
            where.append(" and p.categoria.id in :categorias");
            parametros.put("categorias", categoriaIds);
        }
        if (filtro.modalidad() != null) {
            where.append(" and p.modalidad = :modalidad");
            parametros.put("modalidad", filtro.modalidad());
        }
        if (filtro.precioMinimo() != null) {
            where.append(" and p.precioBase.cantidad >= :precioMinimo");
            parametros.put("precioMinimo", filtro.precioMinimo());
        }
        if (filtro.precioMaximo() != null) {
            where.append(" and p.precioBase.cantidad <= :precioMaximo");
            parametros.put("precioMaximo", filtro.precioMaximo());
        }
        if (filtro.habilidadId() != null) {
            where.append(" and exists (select 1 from p.habilidades h where h.id = :habilidad)");
            parametros.put("habilidad", filtro.habilidadId());
        }
        if (filtro.reputacionMinima() != null) {
            where.append(" and p.autor.reputacion >= :reputacion");
            parametros.put("reputacion", filtro.reputacionMinima());
        }
        String orden = ordenar(filtro.orden(), texto != null);
        return gestor.ejecutar(() -> {
            EntityManager em = gestor.entityManager();
            TypedQuery<Long> cuenta = em.createQuery("select count(p) from Publicacion p" + where, Long.class);
            TypedQuery<Publicacion> consulta = em.createQuery(LISTADO + where + orden, Publicacion.class)
                    .setFirstResult(filtro.pagina() * filtro.tamanio())
                    .setMaxResults(filtro.tamanio());
            parametros.forEach((nombre, valor) -> {
                cuenta.setParameter(nombre, valor);
                consulta.setParameter(nombre, valor);
            });
            long total = cuenta.getSingleResult();
            List<Publicacion> contenido = consulta.getResultList();
            contenido.forEach(PublicacionJpaRepository::inicializarListado);
            return new Pagina<>(contenido, filtro.pagina(), filtro.tamanio(), total);
        });
    }

    /*
     * CriterioOrden -> ORDER BY. Siempre termina en p.id para que la paginacion sea estable.
     * RELEVANCIA: primero las que tienen el texto en el titulo, despues mejor reputacion y mas recientes.
     */
    private static String ordenar(CriterioOrden criterio, boolean hayTexto) {
        if (criterio == null) {
            return " order by p.id";
        }
        return switch (criterio) {
            case MENOR_PRECIO -> " order by p.precioBase.cantidad asc, p.id";
            case MEJOR_REPUTACION -> " order by a.reputacion desc, p.id";
            case MAS_RECIENTE -> " order by p.fechaPublicacion desc, p.id desc";
            case RELEVANCIA -> (hayTexto ? " order by case when lower(p.titulo) like :texto then 0 else 1 end," : " order by")
                    + " a.reputacion desc, p.fechaPublicacion desc, p.id";
        };
    }

    @Override
    public List<Publicacion> listarActivasDe(Long autorId) {
        return listar("where a.id = :autor and p.nombreEstado in (:estados) order by p.id",
                Map.of("autor", autorId, "estados", List.of(EstadoPublicada.NOMBRE, EstadoPausada.NOMBRE)));
    }

    @Override
    public List<Publicacion> listarPublicadas() {
        return listar("where p.nombreEstado = :estado order by p.id", Map.of("estado", EstadoPublicada.NOMBRE));
    }

    /* Todas las del autor, borradores incluidos ("Mis publicaciones"). */
    @Override
    public List<Publicacion> listarDeAutor(Long autorId) {
        return listar("where a.id = :autor order by p.id", Map.of("autor", autorId));
    }

    private List<Publicacion> listar(String condicion, Map<String, Object> parametros) {
        return gestor.ejecutar(() -> {
            TypedQuery<Publicacion> consulta = gestor.entityManager().createQuery(LISTADO + condicion, Publicacion.class);
            parametros.forEach(consulta::setParameter);
            List<Publicacion> publicaciones = consulta.getResultList();
            publicaciones.forEach(PublicacionJpaRepository::inicializarListado);
            return publicaciones;
        });
    }

    /* Colecciones que leen PublicacionDto y la recomendacion por afinidad (se cargan de a lotes). */
    private static void inicializarListado(Publicacion publicacion) {
        publicacion.getEtiquetas().size();
        publicacion.getHabilidades().size();
    }
}
