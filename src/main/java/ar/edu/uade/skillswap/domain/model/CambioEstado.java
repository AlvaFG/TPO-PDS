package ar.edu.uade.skillswap.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

/*
 * Value Object: entrada del historial de estados.
 * Es inmutable (sin setters): el historial solo se agrega, nunca se edita.
 */
@Embeddable
public class CambioEstado {

    @Column(nullable = false)
    private LocalDateTime fecha;
    private String actorId;
    private String estadoAnterior;
    @Column(nullable = false)
    private String estadoNuevo;
    private String motivo;

    protected CambioEstado() {
    }

    public CambioEstado(LocalDateTime fecha, String actorId, String estadoAnterior,
                        String estadoNuevo, String motivo) {
        this.fecha = fecha;
        this.actorId = actorId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getActorId() {
        return actorId;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getMotivo() {
        return motivo;
    }
}
