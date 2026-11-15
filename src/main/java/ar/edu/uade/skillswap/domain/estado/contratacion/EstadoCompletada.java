package ar.edu.uade.skillswap.domain.estado.contratacion;

public class EstadoCompletada extends EstadoContratacionBase {

    public static final String NOMBRE = "COMPLETADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public boolean permiteCalificar() {
        return true;
    }
}
