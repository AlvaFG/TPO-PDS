package ar.edu.uade.skillswap.application.port;

/*
 * Adapter Pattern - Target del servicio de identidad externo.
 */
public interface IServicioIdentidad {

    boolean validarUsuario(String userId);

    String obtenerEmail(String userId);
}
