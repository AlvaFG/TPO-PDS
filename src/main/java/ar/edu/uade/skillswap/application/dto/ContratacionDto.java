package ar.edu.uade.skillswap.application.dto;

import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.PropuestaReprogramacion;

import java.time.LocalDateTime;

/*
 * Resumen y comprobante de una contratacion, visto por un participante:
 * contraparte es el nombre de la otra parte y rol es CLIENTE o PROVEEDOR (el del usuario que consulta).
 * propuestaFecha es el inicio de la franja propuesta en una reprogramacion pendiente (null si no hay).
 */
public record ContratacionDto(Long id, String publicacion, String estado, long precio,
                              LocalDateTime inicio, String detallePrecio, String contraparte, String rol,
                              LocalDateTime propuestaFecha, boolean puedeAceptarPropuesta) {

    public static final String CLIENTE = "CLIENTE";
    public static final String PROVEEDOR = "PROVEEDOR";

    public static ContratacionDto desde(Contratacion c, String userId) {
        PropuestaReprogramacion propuesta = c.getPropuesta();
        boolean hayPropuesta = propuesta != null && propuesta.getNuevaFranja() != null;
        return new ContratacionDto(c.getId(), c.getPublicacion().getTitulo(), c.getNombreEstado(),
                c.getPrecioAcordado().getCantidad(), c.getFranja().getInicio(), c.getDetallePrecio(),
                c.contraparteDe(userId).getNombreVisible(), c.esCliente(userId) ? CLIENTE : PROVEEDOR,
                hayPropuesta ? propuesta.getNuevaFranja().getInicio() : null,
                hayPropuesta && propuesta.puedeAceptarla(userId));
    }
}
