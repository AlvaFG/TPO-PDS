package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.dto.CriterioOrden;
import ar.edu.uade.skillswap.application.dto.FiltroBusqueda;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoBorrador;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPausada;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicada;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Modalidad;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.domain.precio.PromocionMontoFijo;
import ar.edu.uade.skillswap.domain.precio.PromocionPorcentual;
import ar.edu.uade.skillswap.infrastructure.persistence.CatalogoJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PoliticaCancelacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PublicacionJpaRepository;
import ar.edu.uade.skillswap.soporte.Datos;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Busqueda con filtros, orden y paginacion en la consulta, y reconstruccion del State al recargar.
 * Las publicaciones de la prueba estan en una categoria propia, asi los datos de demostracion no interfieren.
 */
class PublicacionJpaRepositoryTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2027, 5, 3, 10, 0);

    private static JpaGestorTransacciones gestor;
    private static PublicacionJpaRepository publicaciones;
    private static List<Long> categoriaIds;
    private static Categoria categoriaDemo;
    private static Perfil autorUno;
    private static Perfil autorDos;
    private static Publicacion kotlin;
    private static Publicacion diseno;
    private static Publicacion testing;
    private static Publicacion borrador;

    @BeforeAll
    static void cargarDatos() {
        gestor = BaseDePrueba.nueva();
        publicaciones = new PublicacionJpaRepository(gestor);
        PerfilJpaRepository perfiles = new PerfilJpaRepository(gestor);
        CatalogoJpaRepository catalogo = new CatalogoJpaRepository(gestor);

        Categoria raiz = new Categoria("Programacion movil");
        Categoria sub = new Categoria("Android");
        raiz.agregar(sub);
        gestor.ejecutar(() -> {
            gestor.entityManager().persist(raiz);
            return raiz;
        });
        categoriaIds = catalogo.buscarCategoria(raiz.getId()).orElseThrow().listarSubcategoriaIds();
        Habilidad java = catalogo.buscarHabilidades(List.of(10L)).get(0);
        Habilidad ux = catalogo.buscarHabilidades(List.of(15L)).get(0);

        autorUno = perfiles.guardar(Datos.profesional(BaseDePrueba.unico("autor")));
        autorDos = perfiles.guardar(Datos.profesional(BaseDePrueba.unico("autor")));
        fijarReputacion(autorUno, 4.0);
        fijarReputacion(autorDos, 5.0);

        kotlin = publicaciones.guardar(publicada(autorUno, "Kotlin avanzado", sub, Modalidad.REMOTA, 100,
                List.of(java), Set.of("android"), 1));
        diseno = publicaciones.guardar(publicada(autorDos, "Diseño de interfaces", raiz, Modalidad.PRESENCIAL, 300,
                List.of(ux), Set.of("figma"), 2));
        testing = publicaciones.guardar(publicada(autorUno, "Testing en Android", sub, Modalidad.REMOTA, 200,
                List.of(java), Set.of("junit"), 3));
        borrador = publicaciones.guardar(Datos.borrador(autorUno, sub, 50, 1, null, List.of(java), INICIO));
        // Publicada pero en otra categoria: no aparece al filtrar por la categoria de la prueba
        categoriaDemo = catalogo.buscarCategoria(4L).orElseThrow();
        publicaciones.guardar(publicada(autorUno, "Kotlin para web", categoriaDemo,
                Modalidad.REMOTA, 10, List.of(java), Set.of(), 4));
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    private static void fijarReputacion(Perfil perfil, double reputacion) {
        gestor.ejecutar(() -> gestor.entityManager()
                .createNativeQuery("update perfil set reputacion = ?1 where id = ?2")
                .setParameter(1, reputacion).setParameter(2, perfil.getId()).executeUpdate());
    }

    private static Publicacion publicada(Perfil autor, String titulo, Categoria categoria, Modalidad modalidad,
                                         long precio, List<Habilidad> habilidades, Set<String> etiquetas, int dia) {
        Publicacion p = new Publicacion(autor, TipoPublicacion.MENTORIA, titulo, "Descripcion de " + titulo, categoria,
                habilidades, modalidad, 60, 2, new SkillCoins(precio), etiquetas, null);
        p.agregarFranja(new FranjaHoraria(INICIO, INICIO.plusHours(1)));
        p.publicar(autor.getUserId(), LocalDateTime.of(2026, 11, dia, 9, 0));
        return p;
    }

    private static FiltroBusqueda filtro(String texto, Modalidad modalidad, Long min, Long max, Long habilidad,
                                         Double reputacion, CriterioOrden orden, int pagina, int tamanio) {
        return new FiltroBusqueda(texto, null, modalidad, min, max, habilidad, reputacion, orden, pagina, tamanio);
    }

    private static List<Long> ids(Pagina<Publicacion> pagina) {
        return pagina.contenido().stream().map(Publicacion::getId).toList();
    }

    private static Pagina<Publicacion> buscar(FiltroBusqueda filtro) {
        return publicaciones.buscar(filtro, categoriaIds);
    }

    @Test
    void laCategoriaIncluyeSusSubcategoriasYSoloDevuelvePublicadas() {
        Pagina<Publicacion> pagina = buscar(filtro(null, null, null, null, null, null, null, 0, 10));
        assertEquals(List.of(kotlin.getId(), diseno.getId(), testing.getId()), ids(pagina));
        assertEquals(3, pagina.totalElementos());
        assertFalse(ids(pagina).contains(borrador.getId()));
    }

    @Test
    void filtraPorTextoEnTituloDescripcionYEtiquetas() {
        assertEquals(List.of(kotlin.getId()), ids(buscar(filtro("KOTLIN", null, null, null, null, null, null, 0, 10))));
        assertEquals(List.of(diseno.getId()), ids(buscar(filtro("figma", null, null, null, null, null, null, 0, 10))));
        assertEquals(List.of(testing.getId()), ids(buscar(filtro("descripcion de testing", null, null, null, null, null, null, 0, 10))));
    }

    @Test
    void filtraPorModalidadPrecioHabilidadYReputacion() {
        assertEquals(List.of(diseno.getId()), ids(buscar(filtro(null, Modalidad.PRESENCIAL, null, null, null, null, null, 0, 10))));
        assertEquals(List.of(kotlin.getId(), testing.getId()), ids(buscar(filtro(null, null, 100L, 200L, null, null, null, 0, 10))));
        assertEquals(List.of(diseno.getId()), ids(buscar(filtro(null, null, null, null, 15L, null, null, 0, 10))));
        assertEquals(List.of(diseno.getId()), ids(buscar(filtro(null, null, null, null, null, 4.5, null, 0, 10))));
    }

    @Test
    void ordenaEnLaConsulta() {
        assertEquals(List.of(kotlin.getId(), testing.getId(), diseno.getId()),
                ids(buscar(filtro(null, null, null, null, null, null, CriterioOrden.MENOR_PRECIO, 0, 10))));
        assertEquals(List.of(diseno.getId(), kotlin.getId(), testing.getId()),
                ids(buscar(filtro(null, null, null, null, null, null, CriterioOrden.MEJOR_REPUTACION, 0, 10))));
        assertEquals(List.of(testing.getId(), diseno.getId(), kotlin.getId()),
                ids(buscar(filtro(null, null, null, null, null, null, CriterioOrden.MAS_RECIENTE, 0, 10))));
        // Relevancia: primero las que tienen el texto en el titulo
        assertEquals(List.of(testing.getId(), kotlin.getId()),
                ids(buscar(filtro("android", null, null, null, null, null, CriterioOrden.RELEVANCIA, 0, 10))));
    }

    @Test
    void paginaEnLaConsultaConElTotal() {
        Pagina<Publicacion> segunda = buscar(filtro(null, null, null, null, null, null, CriterioOrden.MENOR_PRECIO, 1, 2));
        assertEquals(List.of(diseno.getId()), ids(segunda));
        assertEquals(3, segunda.totalElementos());
        assertEquals(2, segunda.totalPaginas());
    }

    @Test
    void elResultadoSeConvierteADtoFueraDeLaTransaccion() {
        PublicacionDto dto = buscar(filtro("kotlin", null, null, null, null, null, null, 0, 10))
                .map(PublicacionDto::desde).contenido().get(0);
        assertEquals("Android", dto.categoria());
        assertEquals(autorUno.getNombreVisible(), dto.autor());
        assertEquals(4.0, dto.reputacionAutor());
        assertEquals(List.of("android"), dto.etiquetas());
    }

    @Test
    void listadosPorAutorYPublicadas() {
        List<Long> activas = publicaciones.listarActivasDe(autorUno.getId()).stream().map(Publicacion::getId).toList();
        assertTrue(activas.containsAll(List.of(kotlin.getId(), testing.getId())));
        assertFalse(activas.contains(diseno.getId()));
        assertTrue(publicaciones.listarDeAutor(autorUno.getId()).stream().anyMatch(p -> p.getId().equals(borrador.getId())));
        assertFalse(publicaciones.listarActivasDe(autorUno.getId()).stream().anyMatch(p -> p.getId().equals(borrador.getId())));
        List<Publicacion> publicadas = publicaciones.listarPublicadas();
        assertTrue(publicadas.stream().allMatch(Publicacion::admiteSolicitudes));
        assertTrue(publicadas.stream().anyMatch(p -> p.getId().equals(kotlin.getId()) && !p.getHabilidades().isEmpty()));
    }

    @Test
    void alRecargarSeReconstruyeElEstadoYElHistorial() {
        Long id = publicaciones.guardar(Datos.borrador(autorDos, categoriaDemo, 50, 1, null, List.of(), INICIO)).getId();
        Publicacion leida = publicaciones.buscarPorId(id).orElseThrow();
        assertInstanceOf(EstadoBorrador.class, leida.getEstado());

        gestor.ejecutar(() -> {
            Publicacion p = publicaciones.buscarPorId(id).orElseThrow();
            p.publicar(p.getAutor().getUserId(), LocalDateTime.of(2026, 11, 20, 9, 0));
            p.pausar(p.getAutor().getUserId());
            return publicaciones.guardar(p);
        });

        Publicacion recargada = publicaciones.buscarPorId(id).orElseThrow();
        assertInstanceOf(EstadoPausada.class, recargada.getEstado());
        assertEquals(EstadoPausada.NOMBRE, recargada.getNombreEstado());
        assertFalse(recargada.admiteSolicitudes());
        assertEquals(List.of(EstadoPublicada.NOMBRE, EstadoPausada.NOMBRE),
                recargada.getHistorial().stream().map(c -> c.getEstadoNuevo()).toList());
        assertEquals(1, recargada.getFranjas().size());
    }

    @Test
    void lasPromocionesSePersistenYSeVuelvenADecorarAlRecargar() {
        Publicacion p = publicada(autorUno, "Promo", categoriaDemo, Modalidad.REMOTA, 400, List.of(), Set.of(), 5);
        p.agregarPromocion(new PromocionPorcentual("P30", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), 30));
        p.agregarPromocion(new PromocionMontoFijo("M500", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), new SkillCoins(500)));
        p.agregarPromocion(new PromocionPorcentual("VENCIDA", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2), 50));
        Long id = publicaciones.guardar(p).getId();

        Publicacion leida = publicaciones.buscarPorId(id).orElseThrow();
        assertEquals(3, leida.getPromociones().size());
        assertEquals(new SkillCoins(200), leida.cotizar(LocalDate.of(2025, 1, 1)).calcularPrecio());
        assertEquals(SkillCoins.CERO, leida.cotizar(LocalDate.of(2026, 11, 20)).calcularPrecio());
    }

    @Test
    void laPoliticaElegidaQuedaAsociada() {
        var politica = new PoliticaCancelacionJpaRepository(gestor).buscarPorId(2L).orElseThrow();
        Publicacion p = Datos.borrador(autorDos, null, 10, 1, politica, List.of(), INICIO);
        Long id = publicaciones.guardar(p).getId();
        assertEquals("Estricta", publicaciones.buscarPorId(id).orElseThrow().getPoliticaCancelacion().getNombre());
    }
}
