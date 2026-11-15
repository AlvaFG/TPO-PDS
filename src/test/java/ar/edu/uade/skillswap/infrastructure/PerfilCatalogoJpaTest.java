package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.ElementoCatalogo;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.RangoHorario;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;
import ar.edu.uade.skillswap.domain.politica.PoliticaEstricta;
import ar.edu.uade.skillswap.domain.politica.PoliticaFlexible;
import ar.edu.uade.skillswap.infrastructure.persistence.BilleteraJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CatalogoJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PoliticaCancelacionJpaRepository;
import ar.edu.uade.skillswap.soporte.Datos;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/* Perfil, billetera, catalogo (Composite) y politicas (Strategy) contra H2 con las migraciones. */
class PerfilCatalogoJpaTest {

    private static JpaGestorTransacciones gestor;
    private static PerfilJpaRepository perfiles;
    private static CatalogoJpaRepository catalogo;
    private static BilleteraJpaRepository billeteras;

    @BeforeAll
    static void crearBase() {
        gestor = BaseDePrueba.nueva();
        perfiles = new PerfilJpaRepository(gestor);
        catalogo = new CatalogoJpaRepository(gestor);
        billeteras = new BilleteraJpaRepository(gestor);
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    @Test
    void guardaYLeePerfilConSusColeccionesFueraDeLaTransaccion() {
        List<Habilidad> habilidades = catalogo.buscarHabilidades(List.of(10L, 12L));
        String userId = BaseDePrueba.unico("perfil");
        Perfil perfil = Datos.profesional(userId);
        perfil.actualizarDatos("Ana", "Bio", ZoneId.of("America/Argentina/Buenos_Aires"));
        perfil.definirHabilidades(habilidades.subList(0, 1), habilidades.subList(1, 2));
        perfil.definirDisponibilidad(List.of(new RangoHorario(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
        perfil.elegirCanales(Set.of(CanalNotificacion.EMAIL, CanalNotificacion.PUSH));
        perfiles.guardar(perfil);

        Perfil leido = perfiles.buscarPorUserId(userId).orElseThrow();
        assertEquals("Ana", leido.getNombreVisible());
        assertEquals(ZoneId.of("America/Argentina/Buenos_Aires"), leido.getZonaHoraria());
        assertEquals("Java", leido.getHabilidades().get(0).getNombre());
        assertEquals("Clean Architecture", leido.getIntereses().get(0).getNombre());
        assertEquals(DayOfWeek.MONDAY, leido.getDisponibilidad().get(0).getDia());
        assertEquals(Set.of(CanalNotificacion.EMAIL, CanalNotificacion.PUSH), leido.getCanales());
        assertEquals(leido.getUserId(), perfiles.buscarPorId(leido.getId()).orElseThrow().getUserId());
    }

    @Test
    void laBajaEsLogicaYLaVersionAumenta() {
        String userId = BaseDePrueba.unico("baja");
        Perfil perfil = perfiles.guardar(Datos.profesional(userId));
        long version = perfil.getVersion();
        perfil.darDeBaja();
        perfiles.guardar(perfil);

        Perfil leido = perfiles.buscarPorUserId(userId).orElseThrow();
        assertFalse(leido.isActivo());
        assertTrue(leido.getVersion() > version);
    }

    @Test
    void elUserIdEsUnico() {
        String userId = BaseDePrueba.unico("repetido");
        perfiles.guardar(Datos.profesional(userId));
        assertThrows(PersistenceException.class, () -> perfiles.guardar(Datos.profesional(userId)));
    }

    @Test
    void unaVersionVencidaNoPisaUnCambioPosterior() {
        String userId = BaseDePrueba.unico("version");
        perfiles.guardar(Datos.profesional(userId));
        Perfil copiaA = perfiles.buscarPorUserId(userId).orElseThrow();
        Perfil copiaB = perfiles.buscarPorUserId(userId).orElseThrow();
        copiaA.actualizarDatos("Primero", null, null);
        perfiles.guardar(copiaA);
        copiaB.actualizarDatos("Segundo", null, null);
        var conflicto = assertThrows(ar.edu.uade.skillswap.application.exception.ConflictoConcurrenciaException.class, () -> perfiles.guardar(copiaB));
        assertInstanceOf(jakarta.persistence.OptimisticLockException.class, conflicto.getCause());
        assertEquals("Primero", perfiles.buscarPorUserId(userId).orElseThrow().getNombreVisible());
    }

    @Test
    void laBilleteraSeGuardaConSusMovimientos() {
        String userId = BaseDePrueba.unico("billetera");
        Perfil perfil = perfiles.guardar(Datos.profesional(userId));
        Billetera billetera = new Billetera(perfil, new SkillCoins(100));
        billetera.acreditar(new SkillCoins(50), TipoMovimiento.CREDITO, "Carga", null);
        billeteras.guardar(billetera);

        Billetera leida = billeteras.buscarPorUserId(userId).orElseThrow();
        leida.debitar(new SkillCoins(30), "Compra", null);
        billeteras.guardar(leida); // fuera de transaccion: merge en su propio EntityManager

        Billetera final_ = billeteras.buscarPorUserId(userId).orElseThrow();
        assertEquals(new SkillCoins(120), final_.getSaldo());
        assertEquals(List.of(TipoMovimiento.CREDITO, TipoMovimiento.DEBITO),
                final_.getMovimientos().stream().map(m -> m.getTipo()).toList());
        assertEquals(userId, final_.getTitular().getUserId());
    }

    @Test
    void laCategoriaSeCargaConTodoSuSubarbol() {
        Categoria tecnologia = catalogo.buscarCategoria(1L).orElseThrow();
        assertEquals("Tecnología", tecnologia.getNombre());
        assertEquals(List.of(1L, 2L, 3L), tecnologia.listarSubcategoriaIds());
        assertEquals(List.of("Java", "Entrevistas técnicas", "Clean Architecture", "SOLID", "Microservicios"),
                tecnologia.listarHabilidades().stream().map(ElementoCatalogo::getNombre).toList());
        Habilidad java = catalogo.buscarHabilidades(List.of(10L)).get(0);
        assertTrue(tecnologia.contiene(java));
        assertTrue(catalogo.buscarCategoria(10L).isEmpty(), "una habilidad no es una categoria");
    }

    @Test
    void unaCategoriaNuevaConHijosSePersisteEnCascada() {
        Categoria raiz = new Categoria(BaseDePrueba.unico("Idiomas"));
        Categoria ingles = new Categoria("Ingles");
        ingles.agregar(new Habilidad("Conversacion"));
        raiz.agregar(ingles);
        gestor.ejecutar(() -> {
            gestor.entityManager().persist(raiz);
            return raiz;
        });

        Categoria leida = catalogo.buscarCategoria(raiz.getId()).orElseThrow();
        assertEquals(2, leida.listarSubcategoriaIds().size());
        assertEquals("Conversacion", leida.listarHabilidades().get(0).getNombre());
        assertTrue(catalogo.listarCategorias().stream().anyMatch(c -> c.getId().equals(raiz.getId())));
    }

    @Test
    void listarCategoriasDevuelveLasDeLaDemo() {
        List<String> nombres = catalogo.listarCategorias().stream().map(Categoria::getNombre).toList();
        assertTrue(nombres.containsAll(List.of("Tecnología", "Desarrollo backend", "Diseño", "Negocios")));
        assertTrue(catalogo.buscarHabilidades(List.of()).isEmpty());
    }

    @Test
    void laPoliticaSeReconstruyeComoSuSubclaseConSusUmbrales() {
        PoliticaCancelacionJpaRepository politicas = new PoliticaCancelacionJpaRepository(gestor);
        PoliticaCancelacion flexible = politicas.buscarPorId(1L).orElseThrow();
        PoliticaCancelacion estricta = politicas.buscarPorId(2L).orElseThrow();
        LocalDateTime inicio = LocalDateTime.of(2027, 3, 1, 18, 0);
        SkillCoins pagado = new SkillCoins(100);

        assertInstanceOf(PoliticaFlexible.class, flexible);
        assertInstanceOf(PoliticaEstricta.class, estricta);
        assertEquals(pagado, flexible.calcularReintegro(pagado, inicio, inicio.minusHours(30)));
        assertEquals(new SkillCoins(50), estricta.calcularReintegro(pagado, inicio, inicio.minusHours(72)));
        assertEquals(SkillCoins.CERO, estricta.calcularReintegro(pagado, inicio, inicio.minusHours(71)));
        assertTrue(politicas.buscarPorId(999L).isEmpty());
    }
}
