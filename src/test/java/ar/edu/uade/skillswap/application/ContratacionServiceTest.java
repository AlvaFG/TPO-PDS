package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.dto.ContratacionDto;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.SolicitarCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.service.ContratacionService;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoEvento;
import ar.edu.uade.skillswap.domain.politica.PoliticaEstricta;
import ar.edu.uade.skillswap.domain.politica.PoliticaFlexible;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

/* Casos de uso de la contratacion con puertos simulados en memoria. */
class ContratacionServiceTest {

    private final Repositorios.Perfiles perfiles = new Repositorios.Perfiles();
    private final Repositorios.Publicaciones publicaciones = new Repositorios.Publicaciones();
    private final Repositorios.Contrataciones contrataciones = new Repositorios.Contrataciones();
    private final Repositorios.Billeteras billeteras = new Repositorios.Billeteras();
    private final Repositorios.GestorTransacciones gestor = new Repositorios.GestorTransacciones();
    private final List<EventoContratacion> eventos = new ArrayList<>();
    private ContratacionService service;
    private Publicacion publicacion;
    private Billetera billeteraCliente;
    private Billetera billeteraProveedor;

    @BeforeEach
    void preparar() {
        PublicadorEventos publicador = new PublicadorEventos();
        publicador.suscribir(eventos::add);
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(INICIO.minusDays(5).atZone(zona).toInstant(), zona);
        service = new ContratacionService(contrataciones, publicaciones, perfiles, billeteras, gestor, publicador, reloj);

        Perfil proveedor = perfiles.guardar(Datos.profesional("prov"));
        Perfil cliente = perfiles.guardar(Datos.profesional("cli"));
        perfiles.guardar(Datos.profesional("cli2"));
        billeteraCliente = billeteras.guardar(new Billetera(cliente, new SkillCoins(1500)));
        billeteraProveedor = billeteras.guardar(new Billetera(proveedor, SkillCoins.CERO));
        publicacion = publicaciones.guardar(Datos.publicada(proveedor, 1000, 1, new PoliticaFlexible(),
                INICIO, INICIO.plusDays(1)));
    }

    private Long franja(int indice) {
        return publicacion.getFranjas().get(indice).getId();
    }

    private Long solicitar(String userId, int franja) {
        return service.solicitar(userId, new SolicitarCommand(publicacion.getId(), franja(franja), "Hola", "Practicar"));
    }

    private List<TipoEvento> tiposPublicados() {
        return eventos.stream().map(EventoContratacion::getTipo).toList();
    }

    @Test
    void solicitarAceptarPagarYCompletar() {
        Long id = solicitar("cli", 0);
        service.aceptar("prov", id);
        service.pagar("cli", id);
        service.iniciar("prov", id);
        service.completar("prov", id);

        assertEquals("COMPLETADA", service.verComprobante("cli", id).estado());
        assertEquals(new SkillCoins(500), billeteraCliente.getSaldo());
        assertEquals(new SkillCoins(1000), billeteraProveedor.getSaldo());
        assertEquals(List.of(TipoEvento.SOLICITUD_RECIBIDA, TipoEvento.SOLICITUD_ACEPTADA,
                TipoEvento.PAGO_CONFIRMADO, TipoEvento.SESION_COMPLETADA), tiposPublicados());
        assertEquals(5, gestor.transacciones);
    }

    @Test
    void elAutorNoPuedeSolicitarSuPropiaPublicacion() {
        assertThrows(ReglaNegocioException.class, () -> solicitar("prov", 0));
        assertTrue(eventos.isEmpty());
    }

    @Test
    void solicitarValidaUsuarioPublicacionYFranja() {
        assertThrows(AccesoDenegadoException.class, () -> solicitar("desconocido", 0));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.solicitar("cli", new SolicitarCommand(99L, franja(0), "m", "o")));
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.solicitar("cli", new SolicitarCommand(publicacion.getId(), 99L, "m", "o")));
    }

    @Test
    void soloElProveedorAceptaRechazaIniciaYCompleta() {
        Long id = solicitar("cli", 0);
        assertThrows(AccesoDenegadoException.class, () -> service.aceptar("cli", id));
        assertThrows(AccesoDenegadoException.class, () -> service.rechazar("cli2", id, "m"));
        assertThrows(AccesoDenegadoException.class, () -> service.iniciar("cli", id));
        assertThrows(AccesoDenegadoException.class, () -> service.completar("cli", id));
        assertThrows(RecursoNoEncontradoException.class, () -> service.aceptar("prov", 99L));
    }

    @Test
    void rechazar() {
        Long id = solicitar("cli", 0);
        service.rechazar("prov", id, "Sin agenda");
        assertEquals("RECHAZADA", service.verComprobante("prov", id).estado());
    }

    @Test
    void evitaQueDosAceptacionesConsumanElUltimoCupo() {
        Long primera = solicitar("cli", 0);
        Long segunda = solicitar("cli2", 1);
        service.aceptar("prov", primera);
        assertThrows(ReglaNegocioException.class, () -> service.aceptar("prov", segunda));
        assertEquals("PENDIENTE", service.verComprobante("cli2", segunda).estado());
        assertEquals(1, publicacion.getCuposOcupados());
    }

    @Test
    void pagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
        perfiles.guardar(Datos.profesional("pobre"));
        billeteras.guardar(new Billetera(perfiles.buscarPorUserId("pobre").orElseThrow(), new SkillCoins(10)));
        Long id = solicitar("pobre", 0);
        service.aceptar("prov", id);
        eventos.clear();

        assertThrows(ReglaNegocioException.class, () -> service.pagar("pobre", id));
        assertEquals(new SkillCoins(10), billeteras.buscarPorUserId("pobre").orElseThrow().getSaldo());
        assertEquals(SkillCoins.CERO, billeteraProveedor.getSaldo());
        assertEquals("ACEPTADA", service.verComprobante("pobre", id).estado());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void noHayDoblePagoNiPagaOtroQueElCliente() {
        Long id = solicitar("cli", 0);
        service.aceptar("prov", id);
        assertThrows(AccesoDenegadoException.class, () -> service.pagar("prov", id));
        service.pagar("cli", id);
        assertThrows(ReglaNegocioException.class, () -> service.pagar("cli", id));
        assertEquals(new SkillCoins(500), billeteraCliente.getSaldo());
    }

    @Test
    void pagarSinBilleteraLanza() {
        perfiles.guardar(Datos.profesional("sinBilletera"));
        Long id = solicitar("sinBilletera", 0);
        service.aceptar("prov", id);
        assertThrows(RecursoNoEncontradoException.class, () -> service.pagar("sinBilletera", id));
    }

    @Test
    void cancelarAplicaLaPoliticaYDevuelveElReintegro() {
        Long id = solicitar("cli", 0);
        service.aceptar("prov", id);
        service.pagar("cli", id);
        long reintegro = service.cancelar("cli", id, "Cambio de planes");
        assertEquals(1000, reintegro);
        assertEquals(new SkillCoins(1500), billeteraCliente.getSaldo());
        assertEquals(0, publicacion.getCuposOcupados());
        assertEquals(TipoEvento.CANCELACION, eventos.get(eventos.size() - 1).getTipo());
        assertThrows(AccesoDenegadoException.class, () -> service.cancelar("cli2", id, "m"));
    }

    /* Caso de aceptacion 6: la misma cancelacion reintegra distinto segun la politica de la publicacion. */
    @Test
    void cancelarConDosPoliticasDistintasReintegraDistinto() {
        Long conFlexible = solicitar("cli", 0);
        service.aceptar("prov", conFlexible);
        service.pagar("cli", conFlexible);
        assertEquals(1000, service.cancelar("cli", conFlexible, "Cambio de planes")); // flexible: 100 %

        Publicacion estricta = publicaciones.guardar(Datos.publicada(perfiles.buscarPorUserId("prov").orElseThrow(),
                1000, 1, new PoliticaEstricta(), INICIO, INICIO.plusDays(1)));
        Long conEstricta = service.solicitar("cli", new SolicitarCommand(estricta.getId(),
                estricta.getFranjas().get(0).getId(), "Hola", "Practicar"));
        service.aceptar("prov", conEstricta);
        service.pagar("cli", conEstricta);
        assertEquals(500, service.cancelar("cli", conEstricta, "Cambio de planes")); // estricta: 50 % con 5 dias
    }

    @Test
    void reprogramarConAcuerdoDeAmbasPartes() {
        Long id = solicitar("cli", 0);
        service.aceptar("prov", id);
        service.proponerReprogramacion("cli", id, franja(1));
        service.aceptarReprogramacion("prov", id);
        assertEquals(INICIO.plusDays(1), service.verComprobante("cli", id).inicio());
        assertEquals(2, tiposPublicados().stream().filter(TipoEvento.REPROGRAMACION::equals).count());
        assertThrows(AccesoDenegadoException.class, () -> service.proponerReprogramacion("cli2", id, franja(0)));
    }

    @Test
    void listarYComprobante() {
        Long id = solicitar("cli", 0);
        Pagina<ContratacionDto> pagina = service.listar("cli", 0, 10);
        assertEquals(1, pagina.totalElementos());
        ContratacionDto dto = pagina.contenido().get(0);
        assertEquals(id, dto.id());
        assertEquals("Mentoria de entrevistas", dto.publicacion());
        assertEquals(1000, dto.precio());
        assertEquals("Precio base 1000 SC", dto.detallePrecio());
        assertTrue(service.listar("cli2", 0, 10).contenido().isEmpty());
        assertThrows(AccesoDenegadoException.class, () -> service.verComprobante("cli2", id));
    }

    @Test
    void elComprobanteIndicaRolYContraparteSegunQuienConsulta() {
        Long id = solicitar("cli", 0);
        ContratacionDto cliente = service.verComprobante("cli", id);
        ContratacionDto proveedor = service.verComprobante("prov", id);
        assertEquals(ContratacionDto.CLIENTE, cliente.rol());
        assertEquals("Perfil prov", cliente.contraparte());
        assertEquals(ContratacionDto.PROVEEDOR, proveedor.rol());
        assertEquals("Perfil cli", proveedor.contraparte());
    }

    @Test
    void sinFranjaSolicitaLaPrimeraLibreYFallaSiNoQuedan() {
        Long id = service.solicitar("cli", new SolicitarCommand(publicacion.getId(), null, "Hola", "Practicar"));
        assertEquals(INICIO, service.verComprobante("cli", id).inicio());
        service.aceptar("prov", id);

        Long otro = service.solicitar("cli2", new SolicitarCommand(publicacion.getId(), null, "Hola", "Practicar"));
        assertEquals(INICIO.plusDays(1), service.verComprobante("cli2", otro).inicio());

        publicacion.getFranjas().forEach(f -> { if (f.estaLibre()) f.ocupar(); });
        assertThrows(ReglaNegocioException.class,
                () -> service.solicitar("cli", new SolicitarCommand(publicacion.getId(), null, "Hola", "Practicar")));
    }
}
