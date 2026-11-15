package ar.edu.uade.skillswap.presentation;

/*
 * Error que llega a la UI con un mensaje legible. Reemplaza a las excepciones del dominio y de
 * la aplicacion (regla de negocio, acceso denegado, recurso inexistente) en el limite de la presentacion.
 */
public class ErrorDeAplicacion extends RuntimeException {

    public ErrorDeAplicacion(String mensaje) {
        super(mensaje);
    }
}
