package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoBorrador;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoCerrada;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPausada;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicacionCancelada;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicada;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.CambioEstado;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Modalidad;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.domain.politica.PoliticaFlexible;
import ar.edu.uade.skillswap.soporte.Datos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

/* State de Publicacion (consigna 4.2) e invariantes de publicar y de cupo. */
class PublicacionTest {

    private Perfil autor;
    private Publicacion publicacion;

    @BeforeEach
    void crearBorrador() {
        autor = Datos.profesional("prov");
        publicacion = Datos.borrador(autor, new Categoria("Tec"), 1000, 1, new PoliticaFlexible(), List.of(),
                INICIO, INICIO.plusDays(1));
    }

    private Publicacion incompleta(String titulo, Categoria categoria, SkillCoins precio) {
        return new Publicacion(autor, TipoPublicacion.MENTORIA, titulo, "d", categoria, List.of(), Modalidad.REMOTA,
                60, 1, precio, Set.of(), null);
    }

    @Test
    void borradorEditableYNoVisible() {
        assertEquals(EstadoBorrador.NOMBRE, publicacion.getNombreEstado());
        assertTrue(publicacion.getEstado().esEditable());
        assertFalse(publicacion.admiteSolicitudes());
        assertTrue(publicacion.esVisiblePara("prov"));
        assertFalse(publicacion.esVisiblePara("otro"));
        publicacion.editar("Nuevo titulo", "Nueva", new SkillCoins(500));
        assertEquals("Nuevo titulo", publicacion.getTitulo());
        assertEquals("Nueva", publicacion.getTextoDescripcion());
        assertEquals(new SkillCoins(500), publicacion.calcularPrecio());
    }

    @Test
    void publicarRegistraElCambioEnElHistorial() {
        publicacion.publicar("prov", INICIO.minusDays(5));
        assertEquals(EstadoPublicada.NOMBRE, publicacion.getNombreEstado());
        assertTrue(publicacion.admiteSolicitudes());
        assertTrue(publicacion.esVisiblePara("cualquiera"));
        assertEquals(INICIO.minusDays(5), publicacion.getFechaPublicacion());
        CambioEstado cambio = publicacion.getHistorial().get(0);
        assertEquals("prov", cambio.getActorId());
        assertEquals(EstadoBorrador.NOMBRE, cambio.getEstadoAnterior());
        assertEquals(EstadoPublicada.NOMBRE, cambio.getEstadoNuevo());
        assertNotNull(cambio.getFecha());
    }

    @Test
    void rechazaPublicarUnaOfertaIncompleta() {
        Publicacion sinNada = incompleta(" ", null, null);
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> sinNada.publicar("prov", INICIO));
        assertTrue(e.getMensaje().contains("titulo"));
        assertTrue(e.getMensaje().contains("categoria"));
        assertTrue(e.getMensaje().contains("precio"));
        assertTrue(e.getMensaje().contains("franja"));
        assertEquals(EstadoBorrador.NOMBRE, sinNada.getNombreEstado());
        assertTrue(sinNada.getHistorial().isEmpty());
    }

    @Test
    void sinFranjaNoSePublica() {
        Publicacion sinFranja = incompleta("Titulo", new Categoria("Tec"), new SkillCoins(0));
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> sinFranja.publicar("prov", INICIO));
        assertEquals("Publicacion incompleta, falta: al menos una franja", e.getMensaje());
    }

    @Test
    void pausarReanudarYCerrar() {
        publicacion.publicar("prov", INICIO);
        publicacion.pausar("prov");
        assertEquals(EstadoPausada.NOMBRE, publicacion.getNombreEstado());
        assertFalse(publicacion.admiteSolicitudes());
        assertTrue(publicacion.esVisiblePara("prov"));
        assertFalse(publicacion.esVisiblePara("otro"));
        publicacion.reanudar("prov");
        assertEquals(EstadoPublicada.NOMBRE, publicacion.getNombreEstado());
        publicacion.cerrar("prov");
        assertEquals(EstadoCerrada.NOMBRE, publicacion.getNombreEstado());
        assertFalse(publicacion.esVisiblePara("otro"));
        assertEquals(4, publicacion.getHistorial().size());
    }

    @Test
    void unaPublicacionCerradaNoSeReabre() {
        publicacion.publicar("prov", INICIO);
        publicacion.cerrar("prov");
        assertThrows(ReglaNegocioException.class, () -> publicacion.reanudar("prov"));
        assertThrows(ReglaNegocioException.class, () -> publicacion.publicar("prov", INICIO));
        assertThrows(ReglaNegocioException.class, () -> publicacion.cancelar("prov", "motivo"));
        assertTrue(publicacion.esVisiblePara("prov"));
    }

    @Test
    void transicionesInvalidasDesdeBorrador() {
        assertThrows(ReglaNegocioException.class, () -> publicacion.pausar("prov"));
        assertThrows(ReglaNegocioException.class, () -> publicacion.reanudar("prov"));
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> publicacion.cerrar("prov"));
        assertEquals("Transicion invalida: no se puede cerrar una publicacion en estado BORRADOR", e.getMensaje());
    }

    @Test
    void transicionesInvalidasDesdePublicadaYPausada() {
        publicacion.publicar("prov", INICIO);
        assertThrows(ReglaNegocioException.class, () -> publicacion.publicar("prov", INICIO));
        assertThrows(ReglaNegocioException.class, () -> publicacion.reanudar("prov"));
        publicacion.pausar("prov");
        assertThrows(ReglaNegocioException.class, () -> publicacion.pausar("prov"));
        assertThrows(ReglaNegocioException.class, () -> publicacion.publicar("prov", INICIO));
    }

    @Test
    void cancelarExigeMotivoYQuedaEnElHistorial() {
        assertThrows(ReglaNegocioException.class, () -> publicacion.cancelar("prov", " "));
        publicacion.cancelar("prov", "Ya no la ofrezco");
        assertEquals(EstadoPublicacionCancelada.NOMBRE, publicacion.getNombreEstado());
        assertEquals("Ya no la ofrezco", publicacion.getHistorial().get(0).getMotivo());
        assertTrue(publicacion.esVisiblePara("prov"));
        assertFalse(publicacion.esVisiblePara("otro"));
        assertThrows(ReglaNegocioException.class, () -> publicacion.publicar("prov", INICIO));
    }

    @Test
    void cancelarDesdePublicadaYPausada() {
        publicacion.publicar("prov", INICIO);
        publicacion.cancelar("prov", "motivo");
        assertEquals(EstadoPublicacionCancelada.NOMBRE, publicacion.getNombreEstado());
        Publicacion otra = Datos.publicada(autor, 10, 1, null, INICIO);
        otra.pausar("prov");
        otra.cancelar("prov", "motivo");
        assertEquals(EstadoPublicacionCancelada.NOMBRE, otra.getNombreEstado());
        Publicacion pausadaCerrada = Datos.publicada(autor, 10, 1, null, INICIO);
        pausadaCerrada.pausar("prov");
        pausadaCerrada.cerrar("prov");
        assertEquals(EstadoCerrada.NOMBRE, pausadaCerrada.getNombreEstado());
    }

    @Test
    void soloSeEditaEnBorrador() {
        publicacion.publicar("prov", INICIO);
        assertThrows(ReglaNegocioException.class, () -> publicacion.editar("t", "d", SkillCoins.CERO));
        assertThrows(ReglaNegocioException.class,
                () -> publicacion.agregarFranja(new FranjaHoraria(INICIO.plusDays(5), INICIO.plusDays(5).plusHours(1))));
    }

    @Test
    void noAdmiteFranjasSuperpuestas() {
        assertThrows(ReglaNegocioException.class,
                () -> publicacion.agregarFranja(new FranjaHoraria(INICIO.plusMinutes(30), INICIO.plusHours(2))));
    }

    @Test
    void reservarCupoOcupaLaFranjaYRespetaElCupo() {
        FranjaHoraria primera = publicacion.getFranjas().get(0);
        FranjaHoraria segunda = publicacion.getFranjas().get(1);
        publicacion.reservarCupo(primera);
        assertFalse(primera.estaLibre());
        assertEquals(1, publicacion.getCuposOcupados());
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> publicacion.reservarCupo(segunda));
        assertEquals("Cupo agotado", e.getMensaje());
        assertTrue(segunda.estaLibre());
        publicacion.liberarCupo(primera);
        assertEquals(0, publicacion.getCuposOcupados());
        assertTrue(primera.estaLibre());
    }

    @Test
    void noReservaUnaFranjaAjena() {
        FranjaHoraria ajena = new FranjaHoraria(INICIO, INICIO.plusHours(1));
        assertThrows(ReglaNegocioException.class, () -> publicacion.reservarCupo(ajena));
    }

    @Test
    void reglasDeCreacion() {
        Perfil org = Datos.organizacion("org");
        assertThrows(ReglaNegocioException.class, () -> new Publicacion(org, TipoPublicacion.MENTORIA, "t", "d",
                null, List.of(), Modalidad.REMOTA, 60, 1, SkillCoins.CERO, Set.of(), null));
        assertThrows(ReglaNegocioException.class, () -> new Publicacion(autor, TipoPublicacion.MENTORIA, "t", "d",
                null, List.of(), Modalidad.REMOTA, 60, 0, SkillCoins.CERO, Set.of(), null));
        assertThrows(ReglaNegocioException.class, () -> new Publicacion(null, TipoPublicacion.MENTORIA, "t", "d",
                null, List.of(), Modalidad.REMOTA, 60, 1, SkillCoins.CERO, Set.of(), null));
        Publicacion desafio = new Publicacion(org, TipoPublicacion.DESAFIO, "Reto", "d", null, List.of(),
                Modalidad.PRESENCIAL, 120, 3, new SkillCoins(10), Set.of("hackaton"), null);
        assertEquals(TipoPublicacion.DESAFIO, desafio.getTipo());
        assertEquals(Modalidad.PRESENCIAL, desafio.getModalidad());
        assertEquals(120, desafio.getDuracionMinutos());
        assertEquals(3, desafio.getCupo());
        assertEquals(Set.of("hackaton"), desafio.getEtiquetas());
        assertEquals(org, desafio.getAutor());
        assertNull(desafio.getPoliticaCancelacion());
        assertTrue(desafio.getHabilidades().isEmpty());
        assertEquals(0, desafio.getVersion());
    }

    @Test
    void elHistorialEsInmutable() {
        publicacion.publicar("prov", INICIO);
        assertThrows(UnsupportedOperationException.class, () -> publicacion.getHistorial().clear());
    }
}
