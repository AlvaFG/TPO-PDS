package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.ConfiguracionApp;
import ar.edu.uade.skillswap.ConfiguracionApp.Puertos;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.model.Models.Contratacion;
import ar.edu.uade.skillswap.model.Models.EstadoContratacion;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Movimiento;
import ar.edu.uade.skillswap.model.Models.Notificacion;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/* Controllers de la UI sobre los casos de uso reales, con repositorios en memoria. */
class SkillSwapControllersTest {

    private static final String PROV = "prov";
    private static final String CLI = "cli";

    private final Repositorios.Perfiles perfiles = new Repositorios.Perfiles();
    private final Repositorios.Billeteras billeteras = new Repositorios.Billeteras();
    private final Repositorios.Catalogo catalogo = new Repositorios.Catalogo();
    private final Repositorios.Notificaciones notificaciones = new Repositorios.Notificaciones();
    private UiControllers ui;

    @BeforeEach
    void preparar() {
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(Datos.INICIO.minusDays(10).atZone(zona).toInstant(), zona);
        var proveedor = perfiles.guardar(Datos.profesional(PROV));
        var cliente = perfiles.guardar(Datos.profesional(CLI));
        perfiles.guardar(Datos.profesional("cli2"));
        billeteras.guardar(new Billetera(proveedor, SkillCoins.CERO));
        billeteras.guardar(new Billetera(cliente, new SkillCoins(1500)));
        catalogo.guardar(new Categoria("Tecnologia"));
        catalogo.guardar(new Categoria("Diseno"));
        Set<String> usuarios = Set.of(PROV, CLI, "cli2");
        ui = ConfiguracionApp.crear(new Puertos(perfiles, new Repositorios.Publicaciones(),
                new Repositorios.Contrataciones(), billeteras, notificaciones, new Repositorios.Calificaciones(),
                catalogo, new Repositorios.Politicas(), new Repositorios.GestorTransacciones(),
                new IdentidadFija(usuarios), canal -> notificaciones::guardar, reloj));
    }

    private Publicacion publicar(String titulo, String categoria, Modalidad modalidad, int precio) {
        Publicacion borrador = ui.publicaciones().crearBorrador(PROV, titulo, "Descripcion", categoria, modalidad, 60, precio);
        return ui.publicaciones().cambiarEstado(PROV, borrador.id(), EstadoPublicacion.PUBLICADA);
    }

    private ErrorDeAplicacion error(Runnable accion) {
        return assertThrows(ErrorDeAplicacion.class, accion::run);
    }

    @Test
    void avanzarRecorreElFlujoSegunElRol() {
        Publicacion p = publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        Contratacion c = ui.contrataciones().solicitar(CLI, p.id(), "Practicar");
        assertEquals(EstadoContratacion.PENDIENTE, c.estado());
        assertEquals("Perfil prov", c.contraparte());
        assertEquals(1000, c.precio());

        assertEquals(EstadoContratacion.ACEPTADA, ui.contrataciones().avanzar(PROV, c.id()).estado());
        assertEquals(EstadoContratacion.CONFIRMADA, ui.contrataciones().avanzar(CLI, c.id()).estado());
        assertEquals(EstadoContratacion.EN_CURSO, ui.contrataciones().avanzar(PROV, c.id()).estado());
        Contratacion completada = ui.contrataciones().avanzar(PROV, c.id());
        assertEquals(EstadoContratacion.COMPLETADA, completada.estado());
        assertEquals("Perfil cli", completada.contraparte());

        assertEquals(500, ui.wallet().consultarSaldo(CLI));
        assertEquals(1000, ui.wallet().consultarSaldo(PROV));
        assertEquals(List.of(completada), ui.contrataciones().listarContrataciones(PROV));
    }

    @Test
    void avanzarSinAccionValidaParaElRolEsUnErrorUniforme() {
        Publicacion p = publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        Contratacion c = ui.contrataciones().solicitar(CLI, p.id(), "Practicar");

        assertEquals("No hay una accion disponible para el cliente con la contratacion en estado PENDIENTE",
                error(() -> ui.contrataciones().avanzar(CLI, c.id())).getMessage());
        ui.contrataciones().avanzar(PROV, c.id());
        assertTrue(error(() -> ui.contrataciones().avanzar(PROV, c.id())).getMessage().contains("proveedor"));

        ui.contrataciones().cancelar(CLI, c.id());
        assertTrue(error(() -> ui.contrataciones().avanzar(CLI, c.id())).getMessage().contains("CANCELADA"));
    }

    @Test
    void cancelarReintegraYDejaLaContratacionCancelada() {
        Publicacion p = publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        Contratacion c = ui.contrataciones().solicitar(CLI, p.id(), "Practicar");
        ui.contrataciones().avanzar(PROV, c.id());
        ui.contrataciones().avanzar(CLI, c.id());
        assertEquals(500, ui.wallet().consultarSaldo(CLI));

        Contratacion cancelada = ui.contrataciones().cancelar(CLI, c.id());
        assertEquals(EstadoContratacion.CANCELADA, cancelada.estado());
        assertEquals(1500, ui.wallet().consultarSaldo(CLI));
    }

    @Test
    void solicitarTomaLaPrimeraFranjaLibre() {
        Publicacion p = publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        Contratacion primera = ui.contrataciones().solicitar(CLI, p.id(), "Practicar");
        ui.contrataciones().avanzar(PROV, primera.id());
        Contratacion segunda = ui.contrataciones().solicitar("cli2", p.id(), "Practicar");
        assertEquals(primera.fecha().plusDays(1), segunda.fecha());
    }

    @Test
    void buscarAplicaLosFiltrosDeLaUi() {
        publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        publicar("Taller de redes", "Tecnologia", Modalidad.PRESENCIAL, 300);
        publicar("Portfolio de diseno", "Diseno", Modalidad.REMOTA, 600);
        ui.publicaciones().crearBorrador(PROV, "Borrador oculto", "d", "Diseno", Modalidad.REMOTA, 30, 50);

        assertEquals(3, ui.busqueda().buscar(new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).size());
        assertEquals(List.of("Taller de redes"), titulos(new FiltrosBusqueda("redes", "Todas", null, 0, "Relevancia")));
        assertEquals(List.of("Portfolio de diseno"), titulos(new FiltrosBusqueda(null, "Diseno", null, 0, "Relevancia")));
        assertEquals(List.of("Taller de redes"), titulos(new FiltrosBusqueda("", "Todas", Modalidad.PRESENCIAL, 0, "Relevancia")));
        assertEquals(List.of("Taller de redes", "Portfolio de diseno"),
                titulos(new FiltrosBusqueda("", "Todas", null, 600, "Menor precio")));
        assertEquals("Mentoria Java", titulos(new FiltrosBusqueda("", "Todas", null, 0, "Relevancia")).get(0));
    }

    private List<String> titulos(FiltrosBusqueda filtros) {
        return ui.busqueda().buscar(filtros).stream().map(Publicacion::titulo).toList();
    }

    @Test
    void categoriasIncluyeTodasYElCatalogo() {
        assertEquals(List.of("Todas", "Tecnologia", "Diseno"), ui.busqueda().categorias());
        assertEquals("No existe la categoria Cocina",
                error(() -> ui.busqueda().buscar(new FiltrosBusqueda("", "Cocina", null, 0, null))).getMessage());
    }

    @Test
    void recomendarExcluyeLasPropias() {
        publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        assertEquals(1, ui.busqueda().recomendar(CLI).size());
        assertTrue(ui.busqueda().recomendar(PROV).isEmpty());
    }

    @Test
    void publicacionesSeCreanComoBorradorYCambianDeEstado() {
        Publicacion borrador = ui.publicaciones().crearBorrador(PROV, "Mentoria", "Desc", "Tecnologia",
                Modalidad.PRESENCIAL, 45, 800);
        assertEquals(EstadoPublicacion.BORRADOR, borrador.estado());
        assertEquals(Modalidad.PRESENCIAL, borrador.modalidad());
        assertEquals(45, borrador.duracionMinutos());
        assertEquals("Perfil prov", borrador.proveedor());
        assertEquals(List.of("Tecnologia"), borrador.etiquetas());
        assertEquals(List.of(borrador), ui.publicaciones().listarPropias(PROV));

        String id = borrador.id();
        assertEquals(EstadoPublicacion.PUBLICADA, ui.publicaciones().cambiarEstado(PROV, id, EstadoPublicacion.PUBLICADA).estado());
        assertEquals(EstadoPublicacion.PAUSADA, ui.publicaciones().cambiarEstado(PROV, id, EstadoPublicacion.PAUSADA).estado());
        assertEquals(EstadoPublicacion.PUBLICADA, ui.publicaciones().cambiarEstado(PROV, id, EstadoPublicacion.PUBLICADA).estado());
        assertEquals(EstadoPublicacion.CERRADA, ui.publicaciones().cambiarEstado(PROV, id, EstadoPublicacion.CERRADA).estado());
        assertTrue(error(() -> ui.publicaciones().cambiarEstado(PROV, id, EstadoPublicacion.BORRADOR))
                .getMessage().contains("BORRADOR"));
        assertTrue(ui.publicaciones().listarPropias(CLI).isEmpty());
    }

    @Test
    void crearBorradorValidaCategoriaYModalidad() {
        assertEquals("No existe la categoria Cocina", error(() -> ui.publicaciones()
                .crearBorrador(PROV, "t", "d", "Cocina", Modalidad.REMOTA, 60, 10)).getMessage());
        assertEquals("La modalidad hibrida no esta disponible", error(() -> ui.publicaciones()
                .crearBorrador(PROV, "t", "d", "Tecnologia", Modalidad.HIBRIDA, 60, 10)).getMessage());
    }

    @Test
    void erroresDeNegocioLlegaComoMensajeLegible() {
        Publicacion p = publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        Contratacion c = ui.contrataciones().solicitar(CLI, p.id(), "Practicar");

        // regla de negocio: el autor no puede contratar su propia publicacion
        assertFalse(error(() -> ui.contrataciones().solicitar(PROV, p.id(), "x")).getMessage().isBlank());
        // acceso denegado: un tercero no participa de la contratacion
        assertEquals("El usuario no participa de la contratacion",
                error(() -> ui.contrataciones().avanzar("cli2", c.id())).getMessage());
        // recurso inexistente
        assertEquals("No existe la contratacion 999", error(() -> ui.contrataciones().avanzar(CLI, "999")).getMessage());
        // usuario que identidad no conoce
        assertEquals("El usuario no es valido: fantasma",
                error(() -> ui.wallet().consultarSaldo("fantasma")).getMessage());
        // id con formato invalido
        assertEquals("Identificador invalido: abc", error(() -> ui.contrataciones().cancelar(CLI, "abc")).getMessage());
        assertEquals("Identificador invalido: abc", error(() -> ui.notificaciones().marcarLeida(CLI, "abc")).getMessage());
    }

    @Test
    void walletAcreditaYListaLosMovimientosDelMasNuevoAlMasAntiguo() {
        assertEquals(1500, ui.wallet().consultarSaldo(CLI));
        assertEquals(2000, ui.wallet().acreditarDemo(CLI, 500));
        assertEquals(2000, ui.wallet().consultarSaldo(CLI));

        Publicacion p = publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        Contratacion c = ui.contrataciones().solicitar(CLI, p.id(), "Practicar");
        ui.contrataciones().avanzar(PROV, c.id());
        ui.contrataciones().avanzar(CLI, c.id());

        List<Movimiento> movimientos = ui.wallet().listarMovimientos(CLI);
        assertEquals(2, movimientos.size());
        assertEquals(-1000, movimientos.get(0).importe());
        assertEquals(1000, movimientos.get(0).saldo());
        assertEquals(500, movimientos.get(1).importe());
        assertEquals(2000, movimientos.get(1).saldo());
    }

    @Test
    void notificacionesSeListanYSeMarcanComoLeidas() {
        Publicacion p = publicar("Mentoria Java", "Tecnologia", Modalidad.REMOTA, 1000);
        Contratacion c = ui.contrataciones().solicitar(CLI, p.id(), "Practicar");
        ui.contrataciones().avanzar(PROV, c.id());

        List<Notificacion> delProveedor = ui.notificaciones().listarNotificaciones(PROV);
        assertEquals(1, delProveedor.size());
        assertEquals("Solicitud recibida", delProveedor.get(0).titulo());
        assertEquals("SOLICITUD_RECIBIDA", delProveedor.get(0).tipo());
        assertFalse(delProveedor.get(0).leida());
        assertEquals(1, ui.notificaciones().listarNotificaciones(CLI).size());

        ui.notificaciones().marcarLeida(PROV, delProveedor.get(0).id());
        assertTrue(ui.notificaciones().listarNotificaciones(PROV).get(0).leida());
        assertEquals("La notificacion no es del usuario",
                error(() -> ui.notificaciones().marcarLeida(CLI, delProveedor.get(0).id())).getMessage());

        ui.notificaciones().marcarTodasLeidas(CLI);
        assertTrue(ui.notificaciones().listarNotificaciones(CLI).stream().allMatch(Notificacion::leida));
    }

    @Test
    void verPerfilDelUsuario() {
        var perfil = ui.perfiles().verPerfil(PROV);
        assertEquals(PROV, perfil.id());
        assertEquals("Perfil prov", perfil.nombre());
        assertEquals("Profesional", perfil.rol());
        assertEquals(0, perfil.proyectosCompletados());
    }

    /* Identidad simulada: acepta una lista fija de usuarios. */
    private record IdentidadFija(Set<String> usuarios) implements ar.edu.uade.skillswap.application.port.IServicioIdentidad {
        @Override
        public boolean validarUsuario(String userId) {
            return usuarios.contains(userId);
        }

        @Override
        public String obtenerEmail(String userId) {
            return userId + "@skillswap.test";
        }
    }
}
