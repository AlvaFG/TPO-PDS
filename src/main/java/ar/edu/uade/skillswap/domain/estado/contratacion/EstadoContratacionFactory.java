package ar.edu.uade.skillswap.domain.estado.contratacion;

/*
 * Simple Factory: reconstruye el objeto estado desde el nombre persistido (nombreEstado).
 */
public class EstadoContratacionFactory {

    private EstadoContratacionFactory() {
    }

    public static EstadoContratacion desdeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return new EstadoPendiente();
        }
        return switch (nombre) {
            case EstadoPendiente.NOMBRE -> new EstadoPendiente();
            case EstadoAceptada.NOMBRE -> new EstadoAceptada();
            case EstadoConfirmada.NOMBRE -> new EstadoConfirmada();
            case EstadoEnCurso.NOMBRE -> new EstadoEnCurso();
            case EstadoCompletada.NOMBRE -> new EstadoCompletada();
            case EstadoRechazada.NOMBRE -> new EstadoRechazada();
            case EstadoContratacionCancelada.NOMBRE -> new EstadoContratacionCancelada();
            default -> throw new IllegalArgumentException("Estado de contratacion desconocido: " + nombre);
        };
    }
}
