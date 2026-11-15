package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.SolicitarCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.application.service.ContratacionService;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoAceptada;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoPendiente;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistence.BilleteraJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CatalogoJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.ContratacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PublicacionJpaRepository;
import ar.edu.uade.skillswap.soporte.Datos;
import jakarta.persistence.OptimisticLockException;
import ar.edu.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Caso de aceptacion 4: dos aceptaciones concurrentes no pueden consumir el ultimo cupo.
 * Dos hilos, cada uno con su EntityManager y su transaccion, leen la misma version de la publicacion;
 * una barrera los hace llegar juntos al commit. @Version deja ganar a uno y el otro falla
 * con ConflictoConcurrenciaException (causa OptimisticLockException): el cupo nunca se pasa.
 */
class ConcurrenciaCupoTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2027, 6, 7, 10, 0);

    private static JpaGestorTransacciones gestor;
    private static PerfilJpaRepository perfiles;
    private static PublicacionJpaRepository publicaciones;
    private static ContratacionJpaRepository contrataciones;
    private static BilleteraJpaRepository billeteras;

    @BeforeAll
    static void crearBase() {
        gestor = BaseDePrueba.nueva();
        perfiles = new PerfilJpaRepository(gestor);
        publicaciones = new PublicacionJpaRepository(gestor);
        contrataciones = new ContratacionJpaRepository(gestor);
        billeteras = new BilleteraJpaRepository(gestor);
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    /* Repositorio de publicaciones que hace esperar a los dos hilos antes de guardar (y de confirmar). */
    private static IPublicacionRepository conBarrera(CyclicBarrier barrera) {
        return new IPublicacionRepository() {
            @Override
            public Publicacion guardar(Publicacion publicacion) {
                try {
                    barrera.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return publicaciones.guardar(publicacion);
            }

            @Override
            public Optional<Publicacion> buscarPorId(Long id) {
                return publicaciones.buscarPorId(id);
            }

            @Override
            public Pagina<Publicacion> buscar(FiltroBusqueda filtro, List<Long> categoriaIds) {
                return publicaciones.buscar(filtro, categoriaIds);
            }

            @Override
            public List<Publicacion> listarActivasDe(Long autorId) {
                return publicaciones.listarActivasDe(autorId);
            }

            @Override
            public List<Publicacion> listarPublicadas() {
                return publicaciones.listarPublicadas();
            }
        };
    }

    private static ContratacionService servicio(IPublicacionRepository publicacionRepository) {
        return new ContratacionService(contrataciones, publicacionRepository, perfiles, billeteras, gestor,
                new PublicadorEventos(), Clock.systemDefaultZone());
    }

    /* Publicacion con cupo 1 y dos franjas, y dos solicitudes pendientes (una por franja). */
    private static List<Long> dosSolicitudesParaElUltimoCupo(String proveedorId, Long[] publicacionId) {
        Perfil proveedor = perfiles.guardar(Datos.profesional(proveedorId));
        var categoria = new CatalogoJpaRepository(gestor).buscarCategoria(2L).orElseThrow();
        Publicacion p = Datos.borrador(proveedor, categoria, 100, 1, null, List.of(), INICIO, INICIO.plusDays(1));
        p.publicar(proveedorId, INICIO.minusDays(30));
        Publicacion guardada = publicaciones.guardar(p);
        publicacionId[0] = guardada.getId();
        List<Long> solicitudes = new ArrayList<>();
        for (FranjaHoraria franja : guardada.getFranjas()) {
            Perfil cliente = perfiles.guardar(Datos.profesional(BaseDePrueba.unico("cliente")));
            billeteras.guardar(new Billetera(cliente, new SkillCoins(1000)));
            solicitudes.add(servicio(publicaciones).solicitar(cliente.getUserId(),
                    new SolicitarCommand(guardada.getId(), franja.getId(), "Hola", "Aprender")));
        }
        return solicitudes;
    }

    @Test
    void dosAceptacionesConcurrentesNoConsumenElUltimoCupoDosVeces() throws Exception {
        String proveedor = BaseDePrueba.unico("proveedor");
        Long[] publicacionId = new Long[1];
        List<Long> solicitudes = dosSolicitudesParaElUltimoCupo(proveedor, publicacionId);
        ContratacionService service = servicio(conBarrera(new CyclicBarrier(2)));

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<?>> resultados = new ArrayList<>();
        for (Long id : solicitudes) {
            resultados.add(hilos.submit(() -> service.aceptar(proveedor, id)));
        }
        List<Throwable> fallas = new ArrayList<>();
        for (Future<?> resultado : resultados) {
            try {
                resultado.get(30, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException e) {
                fallas.add(e.getCause());
            }
        }
        hilos.shutdown();

        assertEquals(1, fallas.size(), "una aceptacion gana y la otra falla");
        assertInstanceOf(ConflictoConcurrenciaException.class, fallas.get(0));
        assertInstanceOf(OptimisticLockException.class, fallas.get(0).getCause());

        Publicacion publicacion = publicaciones.buscarPorId(publicacionId[0]).orElseThrow();
        assertEquals(1, publicacion.getCuposOcupados());
        assertEquals(1, publicacion.getFranjas().stream().filter(f -> !f.estaLibre()).count());
        List<String> estados = solicitudes.stream()
                .map(id -> contrataciones.buscarPorId(id).orElseThrow().getEstado().getNombre()).sorted().toList();
        assertEquals(List.of(EstadoAceptada.NOMBRE, EstadoPendiente.NOMBRE), estados);
    }

    @Test
    void despuesDelConflictoReintentarDaCupoAgotado() {
        String proveedor = BaseDePrueba.unico("proveedor");
        Long[] publicacionId = new Long[1];
        List<Long> solicitudes = dosSolicitudesParaElUltimoCupo(proveedor, publicacionId);
        ContratacionService service = servicio(publicaciones);

        service.aceptar(proveedor, solicitudes.get(0));
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.aceptar(proveedor, solicitudes.get(1)));

        assertEquals("Cupo agotado", error.getMessage());
        assertEquals(1, publicaciones.buscarPorId(publicacionId[0]).orElseThrow().getCuposOcupados());
    }
}
