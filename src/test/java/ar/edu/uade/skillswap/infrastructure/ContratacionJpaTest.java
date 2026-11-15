package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.dto.CalificarCommand;
import ar.edu.uade.skillswap.application.dto.ContratacionDto;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.SolicitarCommand;
import ar.edu.uade.skillswap.application.evento.NotificacionObserver;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.evento.ReputacionObserver;
import ar.edu.uade.skillswap.application.port.IBilleteraRepository;
import ar.edu.uade.skillswap.application.service.CalificacionService;
import ar.edu.uade.skillswap.application.service.ContratacionService;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoAceptada;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoCompletada;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoContratacionCancelada;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.CambioEstado;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;
import ar.edu.uade.skillswap.infrastructure.adapter.CanalNotificacionFactory;
import ar.edu.uade.skillswap.infrastructure.adapter.EmailClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentidadAdapter;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentityClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.PushClientMock;
import ar.edu.uade.skillswap.infrastructure.persistence.BilleteraJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CalificacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CatalogoJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.ContratacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.NotificacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PoliticaCancelacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PublicacionJpaRepository;
import ar.edu.uade.skillswap.soporte.Datos;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Flujo de contratacion con los casos de uso reales sobre JPA/H2: limites transaccionales,
 * rollback, doble cobro, calificacion unica, cancelacion con reintegro y reprogramacion.
 */
class ContratacionJpaTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2027, 5, 3, 10, 0);
    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final Clock RELOJ = Clock.fixed(LocalDateTime.of(2026, 11, 20, 10, 0).atZone(ZONA).toInstant(), ZONA);

    private static JpaGestorTransacciones gestor;
    private static PerfilJpaRepository perfiles;
    private static PublicacionJpaRepository publicaciones;
    private static ContratacionJpaRepository contrataciones;
    private static BilleteraJpaRepository billeteras;
    private static CalificacionJpaRepository calificaciones;
    private static NotificacionJpaRepository notificaciones;
    private static PublicadorEventos eventos;
    private static Categoria categoria;

    @BeforeAll
    static void crearBase() {
        gestor = BaseDePrueba.nueva();
        perfiles = new PerfilJpaRepository(gestor);
        publicaciones = new PublicacionJpaRepository(gestor);
        contrataciones = new ContratacionJpaRepository(gestor);
        billeteras = new BilleteraJpaRepository(gestor);
        calificaciones = new CalificacionJpaRepository(gestor);
        notificaciones = new NotificacionJpaRepository(gestor);
        categoria = new CatalogoJpaRepository(gestor).buscarCategoria(2L).orElseThrow();
        eventos = new PublicadorEventos();
        IdentidadAdapter identidad = new IdentidadAdapter(new IdentityClientMock());
        eventos.suscribir(new NotificacionObserver(perfiles, new CanalNotificacionFactory(new EmailClientMock(),
                new PushClientMock(), identidad, notificaciones)));
        eventos.suscribir(new ReputacionObserver(perfiles, calificaciones));
    }

    @AfterAll
    static void cerrar() {
        gestor.getEntityManagerFactory().close();
    }

    /* Proveedor con una mentoria publicada (cupo y franjas dados) y un cliente con saldo. */
    private record Escenario(String proveedor, String cliente, Long publicacionId, List<Long> franjas) {
    }

    private static Escenario escenario(long saldoCliente, int cupo, int cantidadFranjas) {
        Perfil proveedor = perfiles.guardar(Datos.profesional(BaseDePrueba.unico("prov")));
        Perfil cliente = perfiles.guardar(Datos.profesional(BaseDePrueba.unico("cli")));
        billeteras.guardar(new Billetera(proveedor, SkillCoins.CERO));
        billeteras.guardar(new Billetera(cliente, new SkillCoins(saldoCliente)));
        var politica = new PoliticaCancelacionJpaRepository(gestor).buscarPorId(1L).orElseThrow();
        LocalDateTime[] inicios = new LocalDateTime[cantidadFranjas];
        for (int i = 0; i < cantidadFranjas; i++) {
            inicios[i] = INICIO.plusDays(i);
        }
        Publicacion p = Datos.borrador(proveedor, categoria, 1000, cupo, politica, List.of(), inicios);
        p.publicar(proveedor.getUserId(), LocalDateTime.of(2026, 11, 1, 9, 0));
        Publicacion guardada = publicaciones.guardar(p);
        return new Escenario(proveedor.getUserId(), cliente.getUserId(), guardada.getId(),
                guardada.getFranjas().stream().map(f -> f.getId()).toList());
    }

    private static ContratacionService servicio(IBilleteraRepository billeteraRepository) {
        return new ContratacionService(contrataciones, publicaciones, perfiles, billeteraRepository, gestor, eventos, RELOJ);
    }

    private static Long solicitar(ContratacionService service, Escenario e, int franja) {
        return service.solicitar(e.cliente(), new SolicitarCommand(e.publicacionId(), e.franjas().get(franja), "Hola", "Aprender"));
    }

    private static long saldo(String userId) {
        return billeteras.buscarPorUserId(userId).orElseThrow().getSaldo().getCantidad();
    }

    private static Contratacion recargar(Long id) {
        return contrataciones.buscarPorId(id).orElseThrow();
    }

    @Test
    void solicitarAceptarPagarCompletarYCalificarQuedaPersistido() {
        Escenario e = escenario(1500, 1, 1);
        ContratacionService service = servicio(billeteras);
        CalificacionService calificacion = new CalificacionService(contrataciones, calificaciones, gestor, eventos);

        Long id = solicitar(service, e, 0);
        service.aceptar(e.proveedor(), id);
        service.pagar(e.cliente(), id);
        service.iniciar(e.proveedor(), id);
        service.completar(e.proveedor(), id);
        calificacion.calificar(e.cliente(), new CalificarCommand(id, 4, "Muy buena"));

        Contratacion c = recargar(id);
        assertInstanceOf(EstadoCompletada.class, c.getEstado());
        assertEquals(List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA"),
                c.getHistorial().stream().map(CambioEstado::getEstadoNuevo).toList());
        assertEquals(e.proveedor(), c.getHistorial().get(1).getActorId());
        assertTrue(c.isCalificadaPorCliente());
        assertEquals(500, saldo(e.cliente()));
        assertEquals(1000, saldo(e.proveedor()));
        Publicacion p = publicaciones.buscarPorId(e.publicacionId()).orElseThrow();
        assertEquals(1, p.getCuposOcupados());
        assertFalse(p.getFranjas().get(0).estaLibre());

        // ReputacionObserver recalcula fuera de la transaccion; la bandeja interna guarda las notificaciones
        Perfil proveedor = perfiles.buscarPorUserId(e.proveedor()).orElseThrow();
        assertEquals(4.0, proveedor.getReputacion());
        assertEquals(1, calificaciones.listarRecibidas(proveedor.getId()).size());
        assertTrue(notificaciones.listarPorDestinatario(e.cliente(), 0, 10).totalElementos() >= 3);

        // Una sola calificacion por participante: la regla del dominio y la restriccion UNIQUE de la base
        assertThrows(ReglaNegocioException.class,
                () -> calificacion.calificar(e.cliente(), new CalificarCommand(id, 5, "Otra vez")));
        Perfil cliente = perfiles.buscarPorUserId(e.cliente()).orElseThrow();
        assertThrows(PersistenceException.class, () -> gestor.ejecutar(() -> gestor.entityManager().createNativeQuery(
                        "insert into calificacion (contratacion_id, autor_id, calificado_id, puntaje, fecha) "
                                + "values (?1, ?2, ?3, 5, current_timestamp)")
                .setParameter(1, id).setParameter(2, cliente.getId()).setParameter(3, proveedor.getId())
                .executeUpdate()));
    }

    @Test
    void unPagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
        Escenario e = escenario(500, 1, 1);
        ContratacionService service = servicio(billeteras);
        Long id = solicitar(service, e, 0);
        service.aceptar(e.proveedor(), id);

        assertThrows(ReglaNegocioException.class, () -> service.pagar(e.cliente(), id));

        assertInstanceOf(EstadoAceptada.class, recargar(id).getEstado());
        assertEquals(500, saldo(e.cliente()));
        assertEquals(0, saldo(e.proveedor()));
        assertTrue(billeteras.buscarPorUserId(e.cliente()).orElseThrow().getMovimientos().isEmpty());
    }

    @Test
    void siElPagoFallaDespuesDelDebitoElRollbackDejaTodoComoEstaba() {
        Escenario e = escenario(1500, 1, 1);
        Long id = solicitar(servicio(billeteras), e, 0);
        servicio(billeteras).aceptar(e.proveedor(), id);
        long versionAntes = recargar(id).getVersion();

        // Guarda la billetera del cliente, fuerza el UPDATE en la base y falla al guardar la del proveedor
        IBilleteraRepository conFalla = new IBilleteraRepository() {
            private int guardadas;

            @Override
            public Billetera guardar(Billetera billetera) {
                if (++guardadas == 2) {
                    gestor.entityManager().flush();
                    throw new IllegalStateException("falla simulada al acreditar al proveedor");
                }
                return billeteras.guardar(billetera);
            }

            @Override
            public Optional<Billetera> buscarPorUserId(String userId) {
                return billeteras.buscarPorUserId(userId);
            }
        };

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> servicio(conFalla).pagar(e.cliente(), id));
        assertEquals("falla simulada al acreditar al proveedor", error.getMessage());

        Contratacion c = recargar(id);
        assertInstanceOf(EstadoAceptada.class, c.getEstado());
        assertEquals(versionAntes, c.getVersion());
        assertEquals(1500, saldo(e.cliente()));
        assertEquals(0, saldo(e.proveedor()));
        assertTrue(billeteras.buscarPorUserId(e.cliente()).orElseThrow().getMovimientos().isEmpty());
    }

    @Test
    void elDoblePagoLoRechazanElDominioYLaRestriccionUnique() {
        Escenario e = escenario(5000, 1, 1);
        ContratacionService service = servicio(billeteras);
        Long id = solicitar(service, e, 0);
        service.aceptar(e.proveedor(), id);
        service.pagar(e.cliente(), id);

        assertThrows(ReglaNegocioException.class, () -> service.pagar(e.cliente(), id));
        assertEquals(4000, saldo(e.cliente()));

        // Aunque se saltee el dominio, la base no acepta un segundo DEBITO de la misma contratacion
        Long billeteraId = billeteras.buscarPorUserId(e.cliente()).orElseThrow().getId();
        assertThrows(PersistenceException.class, () -> gestor.ejecutar(() -> gestor.entityManager().createNativeQuery(
                        "insert into movimiento_credito (billetera_id, fecha, tipo, monto, concepto, contratacion_id) "
                                + "values (?1, current_timestamp, 'DEBITO', 1000, 'Doble cobro', ?2)")
                .setParameter(1, billeteraId).setParameter(2, id)
                .executeUpdate()));
        assertEquals(1, billeteras.buscarPorUserId(e.cliente()).orElseThrow().getMovimientos().stream()
                .filter(m -> m.getTipo() == TipoMovimiento.DEBITO).count());
    }

    @Test
    void cancelarConAnticipacionReintegraLiberaElCupoYQuedaEnElHistorial() {
        Escenario e = escenario(1500, 1, 1);
        ContratacionService service = servicio(billeteras);
        Long id = solicitar(service, e, 0);
        service.aceptar(e.proveedor(), id);
        service.pagar(e.cliente(), id);

        long reintegro = service.cancelar(e.cliente(), id, "No puedo asistir");

        assertEquals(1000, reintegro); // politica flexible, mas de 24 h antes
        Contratacion c = recargar(id);
        assertInstanceOf(EstadoContratacionCancelada.class, c.getEstado());
        assertEquals("No puedo asistir", c.getHistorial().get(c.getHistorial().size() - 1).getMotivo());
        assertEquals(1500, saldo(e.cliente()));
        assertEquals(0, saldo(e.proveedor()));
        Publicacion p = publicaciones.buscarPorId(e.publicacionId()).orElseThrow();
        assertEquals(0, p.getCuposOcupados());
        assertTrue(p.getFranjas().get(0).estaLibre());
    }

    @Test
    void laReprogramacionPropuestaYAceptadaSePersiste() {
        Escenario e = escenario(1500, 2, 2);
        ContratacionService service = servicio(billeteras);
        Long id = solicitar(service, e, 0);
        service.aceptar(e.proveedor(), id);

        service.proponerReprogramacion(e.cliente(), id, e.franjas().get(1));
        Contratacion conPropuesta = recargar(id);
        assertEquals(e.franjas().get(1), conPropuesta.getPropuesta().getNuevaFranja().getId());
        assertEquals(e.cliente(), conPropuesta.getPropuesta().getPropuestaPor());

        service.aceptarReprogramacion(e.proveedor(), id);
        Contratacion reprogramada = recargar(id);
        assertNull(reprogramada.getPropuesta());
        assertEquals(e.franjas().get(1), reprogramada.getFranja().getId());
        Publicacion p = publicaciones.buscarPorId(e.publicacionId()).orElseThrow();
        assertTrue(p.getFranjas().get(0).estaLibre());
        assertFalse(p.getFranjas().get(1).estaLibre());
    }

    @Test
    void listarPorParticipantePaginaEnLaConsultaSinNMasUno() {
        Escenario e = escenario(1500, 3, 3);
        ContratacionService service = servicio(billeteras);
        Long primera = solicitar(service, e, 0);
        Long segunda = solicitar(service, e, 1);
        Long tercera = solicitar(service, e, 2);

        Statistics estadisticas = gestor.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        Pagina<ContratacionDto> pagina = service.listar(e.proveedor(), 0, 2);
        long consultas = estadisticas.getPrepareStatementCount();
        estadisticas.setStatisticsEnabled(false);

        assertEquals(List.of(tercera, segunda), pagina.contenido().stream().map(ContratacionDto::id).toList());
        assertEquals(3, pagina.totalElementos());
        assertEquals(ContratacionDto.PROVEEDOR, pagina.contenido().get(0).rol());
        assertEquals(INICIO.plusDays(2), pagina.contenido().get(0).inicio());
        assertEquals(2, consultas, "un COUNT y un SELECT con JOIN FETCH, sin una consulta por fila");
        assertEquals(List.of(primera), service.listar(e.cliente(), 1, 2).contenido().stream().map(ContratacionDto::id).toList());

        assertEquals(3L, contrataciones.contarRecientesPorPublicacion(LocalDateTime.now().minusDays(1)).get(e.publicacionId()));
        assertNull(contrataciones.contarRecientesPorPublicacion(LocalDateTime.now().plusDays(1)).get(e.publicacionId()));
    }
}
