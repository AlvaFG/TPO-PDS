package ar.edu.uade.skillswap.application.exception;

/*
 * El userId no existe, no esta activo o no es parte de la operacion.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    public String getMensaje() {
        return getMessage();
    }
}
