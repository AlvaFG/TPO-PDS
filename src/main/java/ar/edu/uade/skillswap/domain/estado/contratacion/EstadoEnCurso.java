package ar.edu.uade.skillswap.domain.estado.contratacion;

import ar.edu.uade.skillswap.domain.model.Contratacion;

public class EstadoEnCurso extends EstadoContratacionBase {

    public static final String NOMBRE = "EN_CURSO";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public void completar(Contratacion c, String actorId) {
        c.cambiarEstado(new EstadoCompletada(), actorId, "Sesion completada");
    }
}
