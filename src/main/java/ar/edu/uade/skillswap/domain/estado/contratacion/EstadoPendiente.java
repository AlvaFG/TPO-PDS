package ar.edu.uade.skillswap.domain.estado.contratacion;

import ar.edu.uade.skillswap.domain.model.Contratacion;

public class EstadoPendiente extends EstadoContratacionBase {

    public static final String NOMBRE = "PENDIENTE";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    /* La aceptacion reserva la franja y el cupo de la publicacion. */
    @Override
    public void aceptar(Contratacion c, String actorId) {
        c.getPublicacion().reservarCupo(c.getFranja());
        c.cambiarEstado(new EstadoAceptada(), actorId, "Aceptada");
    }

    @Override
    public void rechazar(Contratacion c, String actorId, String motivo) {
        c.cambiarEstado(new EstadoRechazada(), actorId, motivo);
    }
}
