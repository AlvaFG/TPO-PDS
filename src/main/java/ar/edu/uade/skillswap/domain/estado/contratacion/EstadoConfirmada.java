package ar.edu.uade.skillswap.domain.estado.contratacion;

import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;

import java.time.LocalDateTime;

public class EstadoConfirmada extends EstadoContratacionBase {

    public static final String NOMBRE = "CONFIRMADA";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public void iniciar(Contratacion c, String actorId) {
        c.cambiarEstado(new EstadoEnCurso(), actorId, "Sesion iniciada");
    }

    @Override
    public void proponerReprogramacion(Contratacion c, String actorId, FranjaHoraria franja) {
        c.registrarPropuesta(actorId, franja);
    }

    @Override
    public void aceptarReprogramacion(Contratacion c, String actorId) {
        c.aplicarPropuesta(actorId);
    }

    /*
     * Ya se pago: si cancela el proveedor se reintegra el 100 %;
     * si cancela el cliente decide la politica de la publicacion (Strategy).
     */
    @Override
    public SkillCoins cancelar(Contratacion c, String actorId, String motivo, LocalDateTime ahora,
                               Billetera cliente, Billetera proveedor) {
        SkillCoins reintegro = calcularReintegro(c, actorId, ahora);
        if (reintegro.getCantidad() > 0) {
            String concepto = "Reintegro contratacion " + c.getId();
            proveedor.debitar(reintegro, concepto, c.getId());
            cliente.acreditar(reintegro, TipoMovimiento.REINTEGRO, concepto, c.getId());
        }
        c.getPublicacion().liberarCupo(c.getFranja());
        c.cambiarEstado(new EstadoContratacionCancelada(), actorId, motivo);
        return reintegro;
    }

    private SkillCoins calcularReintegro(Contratacion c, String actorId, LocalDateTime ahora) {
        PoliticaCancelacion politica = c.getPublicacion().getPoliticaCancelacion();
        if (c.esProveedor(actorId) || politica == null) {
            return c.getPrecioAcordado();
        }
        return politica.calcularReintegro(c.getPrecioAcordado(), c.getFranja().getInicio(), ahora);
    }
}
