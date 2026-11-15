package ar.edu.uade.skillswap.domain.estado.contratacion;

import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.SkillCoins;

import java.time.LocalDateTime;

/*
 * State Pattern - Estado de una contratacion (consigna 4.5).
 */
public interface EstadoContratacion {

    String getNombre();

    void aceptar(Contratacion c, String actorId);

    void rechazar(Contratacion c, String actorId, String motivo);

    void pagar(Contratacion c, String actorId, Billetera cliente, Billetera proveedor);

    void proponerReprogramacion(Contratacion c, String actorId, FranjaHoraria franja);

    void aceptarReprogramacion(Contratacion c, String actorId);

    void iniciar(Contratacion c, String actorId);

    void completar(Contratacion c, String actorId);

    SkillCoins cancelar(Contratacion c, String actorId, String motivo, LocalDateTime ahora,
                        Billetera cliente, Billetera proveedor);

    boolean permiteCalificar();
}
