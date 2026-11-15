package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoContratacion;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoContratacionFactory;
import ar.edu.uade.skillswap.domain.estado.contratacion.EstadoPendiente;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoBorrador;
import ar.edu.uade.skillswap.domain.estado.publicacion.EstadoPublicacionFactory;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/* Simple Factory: reconstruye el estado desde el nombre persistido. */
class EstadoFactoryTest {

    @ParameterizedTest
    @ValueSource(strings = {"BORRADOR", "PUBLICADA", "PAUSADA", "CERRADA", "CANCELADA"})
    void reconstruyeCadaEstadoDePublicacion(String nombre) {
        assertEquals(nombre, EstadoPublicacionFactory.desdeNombre(nombre).getNombre());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA", "RECHAZADA", "CANCELADA"})
    void reconstruyeCadaEstadoDeContratacion(String nombre) {
        assertEquals(nombre, EstadoContratacionFactory.desdeNombre(nombre).getNombre());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void sinNombreDevuelveElEstadoInicial(String nombre) {
        assertEquals(EstadoBorrador.NOMBRE, EstadoPublicacionFactory.desdeNombre(nombre).getNombre());
        assertEquals(EstadoPendiente.NOMBRE, EstadoContratacionFactory.desdeNombre(nombre).getNombre());
    }

    @Test
    void nombreDesconocidoLanza() {
        assertThrows(IllegalArgumentException.class, () -> EstadoPublicacionFactory.desdeNombre("X"));
        assertThrows(IllegalArgumentException.class, () -> EstadoContratacionFactory.desdeNombre("X"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECHAZADA", "CANCELADA"})
    void enEstadosFinalesSoloSeConsultaElHistorial(String nombre) {
        EstadoContratacion estado = EstadoContratacionFactory.desdeNombre(nombre);
        assertThrows(ReglaNegocioException.class, () -> estado.aceptar(null, "u"));
        assertThrows(ReglaNegocioException.class, () -> estado.rechazar(null, "u", "m"));
        assertThrows(ReglaNegocioException.class, () -> estado.pagar(null, "u", null, null));
        assertThrows(ReglaNegocioException.class, () -> estado.proponerReprogramacion(null, "u", null));
        assertThrows(ReglaNegocioException.class, () -> estado.aceptarReprogramacion(null, "u"));
        assertThrows(ReglaNegocioException.class, () -> estado.iniciar(null, "u"));
        assertThrows(ReglaNegocioException.class, () -> estado.completar(null, "u"));
        assertThrows(ReglaNegocioException.class, () -> estado.cancelar(null, "u", "m", null, null, null));
        assertFalse(estado.permiteCalificar());
        assertNotNull(SkillCoins.CERO);
    }
}
