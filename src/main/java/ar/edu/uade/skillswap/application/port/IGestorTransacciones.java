package ar.edu.uade.skillswap.application.port;

import java.util.function.Supplier;

/*
 * Limite transaccional de los casos de uso (aceptar, pagar, cancelar, calificar, ...).
 * La implementacion hace commit si la operacion termina bien y rollback si lanza,
 * y relanza la misma excepcion.
 */
public interface IGestorTransacciones {

    <T> T ejecutar(Supplier<T> operacion);
}
