package ar.edu.uade.skillswap.domain.estado.contratacion;

public class EstadoContratacionCancelada extends EstadoContratacionBase {

    public static final String NOMBRE = "CANCELADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }
}
