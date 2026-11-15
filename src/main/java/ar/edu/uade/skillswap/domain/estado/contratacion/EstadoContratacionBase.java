package ar.edu.uade.skillswap.domain.estado.contratacion;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.SkillCoins;

import java.time.LocalDateTime;

/*
 * State Pattern - Estado base.
 * Por defecto toda accion es invalida; cada estado concreto redefine solo lo que permite.
 */
public abstract class EstadoContratacionBase implements EstadoContratacion {

    @Override
    public void aceptar(Contratacion c, String actorId) {
        throw transicionInvalida("aceptar");
    }

    @Override
    public void rechazar(Contratacion c, String actorId, String motivo) {
        throw transicionInvalida("rechazar");
    }

    @Override
    public void pagar(Contratacion c, String actorId, Billetera cliente, Billetera proveedor) {
        throw transicionInvalida("pagar");
    }

    @Override
    public void proponerReprogramacion(Contratacion c, String actorId, FranjaHoraria franja) {
        throw transicionInvalida("proponer una reprogramacion de");
    }

    @Override
    public void aceptarReprogramacion(Contratacion c, String actorId) {
        throw transicionInvalida("aceptar una reprogramacion de");
    }

    @Override
    public void iniciar(Contratacion c, String actorId) {
        throw transicionInvalida("iniciar");
    }

    @Override
    public void completar(Contratacion c, String actorId) {
        throw transicionInvalida("completar");
    }

    @Override
    public SkillCoins cancelar(Contratacion c, String actorId, String motivo, LocalDateTime ahora,
                               Billetera cliente, Billetera proveedor) {
        throw transicionInvalida("cancelar");
    }

    @Override
    public boolean permiteCalificar() {
        return false;
    }

    private ReglaNegocioException transicionInvalida(String accion) {
        return new ReglaNegocioException("Transicion invalida: no se puede " + accion
                + " una contratacion en estado " + getNombre());
    }
}
