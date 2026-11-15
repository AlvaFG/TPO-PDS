package ar.edu.uade.skillswap.domain.estado.contratacion;

public class EstadoRechazada extends EstadoContratacionBase {

    public static final String NOMBRE = "RECHAZADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }
}
