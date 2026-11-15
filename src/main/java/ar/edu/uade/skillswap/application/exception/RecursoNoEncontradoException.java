package ar.edu.uade.skillswap.application.exception;

/*
 * El recurso pedido no existe.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public String getMensaje() {
        return getMessage();
    }
}
