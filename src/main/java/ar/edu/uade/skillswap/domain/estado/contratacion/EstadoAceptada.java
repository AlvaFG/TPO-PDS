package ar.edu.uade.skillswap.domain.estado.contratacion;

import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;

import java.time.LocalDateTime;

public class EstadoAceptada extends EstadoContratacionBase {

    public static final String NOMBRE = "ACEPTADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    /* Debito y credito en el mismo paso: si el debito falla no cambia nada. */
    @Override
    public void pagar(Contratacion c, String actorId, Billetera cliente, Billetera proveedor) {
        String concepto = "Pago contratacion " + c.getId();
        cliente.debitar(c.getPrecioAcordado(), concepto, c.getId());
        proveedor.acreditar(c.getPrecioAcordado(), TipoMovimiento.CREDITO, concepto, c.getId());
        c.cambiarEstado(new EstadoConfirmada(), actorId, "Pago confirmado");
    }

    @Override
    public void proponerReprogramacion(Contratacion c, String actorId, FranjaHoraria franja) {
        c.registrarPropuesta(actorId, franja);
    }

    @Override
    public void aceptarReprogramacion(Contratacion c, String actorId) {
        c.aplicarPropuesta(actorId);
    }

    /* Todavia no se pago: no hay reintegro, solo se libera el cupo. */
    @Override
    public SkillCoins cancelar(Contratacion c, String actorId, String motivo, LocalDateTime ahora,
                               Billetera cliente, Billetera proveedor) {
        c.getPublicacion().liberarCupo(c.getFranja());
        c.cambiarEstado(new EstadoContratacionCancelada(), actorId, motivo);
        return SkillCoins.CERO;
    }
}
