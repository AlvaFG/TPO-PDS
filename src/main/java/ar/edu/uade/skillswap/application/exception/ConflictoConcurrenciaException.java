package ar.edu.uade.skillswap.application.exception;

/*
 * Otra operacion modifico el mismo dato (por ejemplo el ultimo cupo) antes de confirmar esta.
 * La operacion se deshizo completa y se puede reintentar.
 */
public class ConflictoConcurrenciaException extends RuntimeException {

    public ConflictoConcurrenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
