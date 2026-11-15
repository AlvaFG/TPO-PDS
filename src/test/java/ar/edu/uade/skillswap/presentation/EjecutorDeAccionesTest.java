package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import ar.edu.uade.skillswap.application.port.IServicioIdentidad;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/* Un conflicto de concurrencia llega a la UI como mensaje legible, sin exponer JPA. */
class EjecutorDeAccionesTest {

    private final EjecutorDeAcciones ejecutor = new EjecutorDeAcciones(new IServicioIdentidad() {
        @Override public boolean validarUsuario(String userId) { return true; }
        @Override public String obtenerEmail(String userId) { return userId + "@skillswap.test"; }
    });

    @Test
    void elConflictoDeConcurrenciaSeTraduceAUnMensajeLegible() {
        ErrorDeAplicacion error = assertThrows(ErrorDeAplicacion.class, () -> ejecutor.ejecutar("user", (Supplier<Object>) () -> {
            throw new ConflictoConcurrenciaException("conflicto", new OptimisticLockException());
        }));
        assertEquals("Cupo agotado o publicacion modificada por otra operacion, reintente.", error.getMessage());
    }
}
