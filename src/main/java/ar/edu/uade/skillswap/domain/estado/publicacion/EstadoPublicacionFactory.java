package ar.edu.uade.skillswap.domain.estado.publicacion;

/*
 * Simple Factory: reconstruye el objeto estado desde el nombre persistido (nombreEstado).
 */
public class EstadoPublicacionFactory {

    private EstadoPublicacionFactory() {
    }

    public static EstadoPublicacion desdeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return new EstadoBorrador();
        }
        return switch (nombre) {
            case EstadoBorrador.NOMBRE -> new EstadoBorrador();
            case EstadoPublicada.NOMBRE -> new EstadoPublicada();
            case EstadoPausada.NOMBRE -> new EstadoPausada();
            case EstadoCerrada.NOMBRE -> new EstadoCerrada();
            case EstadoPublicacionCancelada.NOMBRE -> new EstadoPublicacionCancelada();
            default -> throw new IllegalArgumentException("Estado de publicacion desconocido: " + nombre);
        };
    }
}
