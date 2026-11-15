package ar.edu.uade.skillswap.domain.exception;

/*
 * La lanza el dominio cuando se viola una regla de negocio:
 * transicion invalida, saldo insuficiente, cupo agotado, etc.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public String getMensaje() {
        return getMessage();
    }
}
