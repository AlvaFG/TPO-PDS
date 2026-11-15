package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoAceptada;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoCompletada;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoConfirmada;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoContratacionCancelada;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoEnCurso;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoPendiente;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoRechazada;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Calificacion;
import ar.edu.uade.skillswap.domain.model.CambioEstado;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;
import ar.edu.uade.skillswap.domain.politica.PoliticaEstricta;
import ar.edu.uade.skillswap.domain.politica.PoliticaFlexible;
import ar.edu.uade.skillswap.domain.precio.PromocionMontoFijo;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.uade.skillswap.soporte.Datos.HOY;
import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

/* State de Contratacion (consigna 4.5), pagos, cancelacion, reprogramacion y calificacion. */
class ContratacionTest {

    private Perfil proveedor;
    private Perfil cliente;
    private Billetera billeteraCliente;
    private Billetera billeteraProveedor;

    @BeforeEach
    void preparar() {
        proveedor = Datos.profesional("prov");
        cliente = Datos.profesional("cli");
        billeteraCliente = new Billetera(cliente, new SkillCoins(1500));
        billeteraProveedor = new Billetera(proveedor, SkillCoins.CERO);
    }

    private Publicacion publicacion(PoliticaCancelacion politica) {
        return Datos.publicada(proveedor, 1000, 1, politica, INICIO, INICIO.plusDays(1));
    }

    private Contratacion solicitar(Publicacion p, int franja) {
        Contratacion c = new Contratacion(p, cliente, p.getFranjas().get(franja), "Hola", "Entrevista", HOY);
        return Ids.asignar(c, (long) (franja + 1));
    }

    private Contratacion confirmada(PoliticaCancelacion politica) {
        Contratacion c = solicitar(publicacion(politica), 0);
        c.aceptar("prov");
        c.pagar("cli", billeteraCliente, billeteraProveedor);
        return c;
    }

    private Contratacion completada() {
        Contratacion c = confirmada(null);
        c.iniciar("prov");
        c.completar("prov");
        return c;
    }

    @Test
    void laSolicitudQuedaPendienteConElPrecioCotizado() {
        Publicacion p = publicacion(null);
        p.agregarPromocion(new PromocionMontoFijo("BIENVENIDA", HOY, HOY, new SkillCoins(200)));
        Contratacion c = solicitar(p, 0);
        assertEquals(EstadoPendiente.NOMBRE, c.getNombreEstado());
        assertEquals(new SkillCoins(800), c.getPrecioAcordado());
        assertEquals("Precio base 1000 SC - 200 SC", c.getDetallePrecio());
        assertSame(proveedor, c.getProveedor());
        assertSame(cliente, c.getCliente());
        assertEquals("Hola", c.getMensaje());
        assertEquals("Entrevista", c.getObjetivo());
        assertEquals(1, c.getHistorial().size());
        assertNull(c.getHistorial().get(0).getEstadoAnterior());
        assertEquals(0, c.getVersion());
    }

    @Test
    void elAutorNoPuedeSolicitarSuPropiaPublicacion() {
        Publicacion p = publicacion(null);
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> new Contratacion(p, proveedor, p.getFranjas().get(0), "m", "o", HOY));
        assertEquals("El autor no puede solicitar su propia publicacion", e.getMensaje());
    }

    @Test
    void unaPublicacionNoPublicadaNoAdmiteSolicitudes() {
        Publicacion p = publicacion(null);
        p.pausar("prov");
        assertThrows(ReglaNegocioException.class, () -> solicitar(p, 0));
    }

    @Test
    void noSeSolicitaUnaFranjaOcupadaNiAjena() {
        Publicacion p = publicacion(null);
        p.getFranjas().get(0).ocupar();
        assertThrows(ReglaNegocioException.class, () -> solicitar(p, 0));
        FranjaHoraria ajena = new FranjaHoraria(INICIO, INICIO.plusHours(1));
        assertThrows(ReglaNegocioException.class, () -> new Contratacion(p, cliente, ajena, "m", "o", HOY));
    }

    @Test
    void aceptarReservaFranjaYCupo() {
        Contratacion c = solicitar(publicacion(null), 0);
        c.aceptar("prov");
        assertEquals(EstadoAceptada.NOMBRE, c.getNombreEstado());
        assertFalse(c.getFranja().estaLibre());
        assertEquals(1, c.getPublicacion().getCuposOcupados());
    }

    @Test
    void dosAceptacionesNoConsumenElUltimoCupo() {
        Publicacion p = publicacion(null);
        Contratacion primera = solicitar(p, 0);
        Contratacion segunda = solicitar(p, 1);
        primera.aceptar("prov");
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> segunda.aceptar("prov"));
        assertEquals("Cupo agotado", e.getMensaje());
        assertEquals(EstadoPendiente.NOMBRE, segunda.getNombreEstado());
        assertEquals(1, p.getCuposOcupados());
    }

    @Test
    void rechazar() {
        Contratacion c = solicitar(publicacion(null), 0);
        c.rechazar("prov", "Sin agenda");
        assertEquals(EstadoRechazada.NOMBRE, c.getNombreEstado());
        assertEquals("Sin agenda", c.getHistorial().get(1).getMotivo());
        assertThrows(ReglaNegocioException.class, () -> c.aceptar("prov"));
        assertThrows(ReglaNegocioException.class, () -> c.cancelar("cli", "m", INICIO, billeteraCliente, billeteraProveedor));
    }

    @Test
    void pagarEnPendienteEsTransicionInvalida() {
        Contratacion c = solicitar(publicacion(null), 0);
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> c.pagar("cli", billeteraCliente, billeteraProveedor));
        assertEquals("Transicion invalida: no se puede pagar una contratacion en estado PENDIENTE", e.getMensaje());
        assertThrows(ReglaNegocioException.class, () -> c.iniciar("prov"));
        assertThrows(ReglaNegocioException.class, () -> c.completar("prov"));
        assertThrows(ReglaNegocioException.class, () -> c.aceptarReprogramacion("prov"));
    }

    @Test
    void pagarConSaldoSuficienteMueveLasDosBilleteras() {
        Contratacion c = confirmada(null);
        assertEquals(EstadoConfirmada.NOMBRE, c.getNombreEstado());
        assertEquals(new SkillCoins(500), billeteraCliente.getSaldo());
        assertEquals(new SkillCoins(1000), billeteraProveedor.getSaldo());
        assertEquals(TipoMovimiento.CREDITO, billeteraProveedor.getMovimientos().get(0).getTipo());
    }

    @Test
    void pagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
        Billetera pobre = new Billetera(cliente, new SkillCoins(999));
        Contratacion c = solicitar(publicacion(null), 0);
        c.aceptar("prov");
        assertThrows(ReglaNegocioException.class, () -> c.pagar("cli", pobre, billeteraProveedor));
        assertEquals(new SkillCoins(999), pobre.getSaldo());
        assertEquals(SkillCoins.CERO, billeteraProveedor.getSaldo());
        assertEquals(EstadoAceptada.NOMBRE, c.getNombreEstado());
    }

    @Test
    void noHayDoblePago() {
        Contratacion c = confirmada(null);
        assertThrows(ReglaNegocioException.class, () -> c.pagar("cli", billeteraCliente, billeteraProveedor));
        assertEquals(new SkillCoins(500), billeteraCliente.getSaldo());
    }

    @Test
    void cancelarAntesDePagarNoReintegraYLiberaElCupo() {
        Contratacion c = solicitar(publicacion(new PoliticaEstricta()), 0);
        c.aceptar("prov");
        SkillCoins reintegro = c.cancelar("cli", "Cambio de planes", INICIO.minusHours(1), billeteraCliente, billeteraProveedor);
        assertEquals(SkillCoins.CERO, reintegro);
        assertEquals(EstadoContratacionCancelada.NOMBRE, c.getNombreEstado());
        assertEquals(0, c.getPublicacion().getCuposOcupados());
        assertTrue(c.getFranja().estaLibre());
    }

    @Test
    void cancelarConPoliticaFlexible() {
        Contratacion c = confirmada(new PoliticaFlexible());
        SkillCoins reintegro = c.cancelar("cli", "m", INICIO.minusHours(10), billeteraCliente, billeteraProveedor);
        assertEquals(new SkillCoins(500), reintegro);
        assertEquals(new SkillCoins(1000), billeteraCliente.getSaldo());
        assertEquals(new SkillCoins(500), billeteraProveedor.getSaldo());
        assertEquals(TipoMovimiento.REINTEGRO, billeteraCliente.getMovimientos().get(1).getTipo());
        assertEquals(0, c.getPublicacion().getCuposOcupados());
    }

    @Test
    void cancelarConPoliticaEstrictaConLaMismaAnticipacion() {
        Contratacion c = confirmada(new PoliticaEstricta());
        SkillCoins reintegro = c.cancelar("cli", "m", INICIO.minusHours(10), billeteraCliente, billeteraProveedor);
        assertEquals(SkillCoins.CERO, reintegro);
        assertEquals(new SkillCoins(500), billeteraCliente.getSaldo());
        assertEquals(new SkillCoins(1000), billeteraProveedor.getSaldo());
        assertEquals(EstadoContratacionCancelada.NOMBRE, c.getNombreEstado());
    }

    @Test
    void siCancelaElProveedorSeReintegraElCienPorCiento() {
        Contratacion c = confirmada(new PoliticaEstricta());
        SkillCoins reintegro = c.cancelar("prov", "Enfermo", INICIO.minusHours(1), billeteraCliente, billeteraProveedor);
        assertEquals(new SkillCoins(1000), reintegro);
        assertEquals(new SkillCoins(1500), billeteraCliente.getSaldo());
        assertEquals(SkillCoins.CERO, billeteraProveedor.getSaldo());
    }

    @Test
    void reprogramacionAceptadaPorLaContraparte() {
        Contratacion c = confirmada(null);
        FranjaHoraria original = c.getFranja();
        FranjaHoraria nueva = c.getPublicacion().getFranjas().get(1);
        c.proponerReprogramacion("cli", nueva);
        assertEquals("cli", c.getPropuesta().getPropuestaPor());
        assertThrows(ReglaNegocioException.class, () -> c.aceptarReprogramacion("cli"));
        c.aceptarReprogramacion("prov");
        assertSame(nueva, c.getFranja());
        assertFalse(nueva.estaLibre());
        assertTrue(original.estaLibre());
        assertNull(c.getPropuesta());
        assertThrows(ReglaNegocioException.class, () -> c.aceptarReprogramacion("prov"));
    }

    @Test
    void reprogramacionEnAceptada() {
        Contratacion c = solicitar(publicacion(null), 0);
        c.aceptar("prov");
        FranjaHoraria nueva = c.getPublicacion().getFranjas().get(1);
        c.proponerReprogramacion("prov", nueva);
        c.aceptarReprogramacion("cli");
        assertSame(nueva, c.getFranja());
        assertThrows(ReglaNegocioException.class, () -> c.proponerReprogramacion("prov", c.getFranja()));
    }

    @Test
    void noSeReprogramaUnaVezIniciada() {
        Contratacion c = confirmada(null);
        c.iniciar("prov");
        assertEquals(EstadoEnCurso.NOMBRE, c.getNombreEstado());
        assertThrows(ReglaNegocioException.class,
                () -> c.proponerReprogramacion("cli", c.getPublicacion().getFranjas().get(1)));
        assertThrows(ReglaNegocioException.class,
                () -> c.cancelar("cli", "m", INICIO, billeteraCliente, billeteraProveedor));
    }

    @Test
    void completarYCalificarUnaVezPorParticipante() {
        Contratacion c = completada();
        assertEquals(EstadoCompletada.NOMBRE, c.getNombreEstado());
        Calificacion delCliente = c.calificar("cli", 5, "Excelente");
        assertSame(proveedor, delCliente.getCalificado());
        assertSame(cliente, delCliente.getAutor());
        assertSame(c, delCliente.getContratacion());
        assertEquals(5, delCliente.getPuntaje());
        assertEquals("Excelente", delCliente.getComentario());
        assertNotNull(delCliente.getFecha());
        assertTrue(c.isCalificadaPorCliente());
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> c.calificar("cli", 4, "Otra"));
        assertEquals("El participante ya califico esta contratacion", e.getMensaje());
        Calificacion delProveedor = c.calificar("prov", 4, "Puntual");
        assertSame(cliente, delProveedor.getCalificado());
        assertTrue(c.isCalificadaPorProveedor());
    }

    @Test
    void calificacionNoAutorizada() {
        Contratacion pendiente = solicitar(publicacion(null), 0);
        assertThrows(ReglaNegocioException.class, () -> pendiente.calificar("cli", 5, "Antes de tiempo"));
        Contratacion c = completada();
        assertThrows(ReglaNegocioException.class, () -> c.calificar("intruso", 5, "x"));
        assertThrows(ReglaNegocioException.class, () -> c.calificar("cli", 6, "fuera de rango"));
        assertFalse(c.isCalificadaPorCliente());
    }

    @Test
    void unaPersonaNoPuedeCalificarseASiMisma() {
        assertThrows(ReglaNegocioException.class,
                () -> new Calificacion(null, cliente, cliente, 5, "yo", INICIO));
    }

    @Test
    void contraparteYParticipantes() {
        Contratacion c = solicitar(publicacion(null), 0);
        assertSame(proveedor, c.contraparteDe("cli"));
        assertSame(cliente, c.contraparteDe("prov"));
        assertThrows(ReglaNegocioException.class, () -> c.contraparteDe("otro"));
        assertTrue(c.esParticipante("cli"));
        assertFalse(c.esParticipante("otro"));
    }

    @Test
    void cadaTransicionQuedaEnElHistorialInmutable() {
        Contratacion c = completada();
        List<String> estados = c.getHistorial().stream().map(CambioEstado::getEstadoNuevo).toList();
        assertEquals(List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA"), estados);
        assertEquals("prov", c.getHistorial().get(1).getActorId());
        assertEquals("cli", c.getHistorial().get(2).getActorId());
        assertThrows(UnsupportedOperationException.class, () -> c.getHistorial().clear());
        assertThrows(ReglaNegocioException.class, () -> c.iniciar("prov"));
        assertTrue(c.getEstado().permiteCalificar());
    }
}
