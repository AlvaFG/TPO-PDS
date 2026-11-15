package ar.edu.uade.skillswap.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/*
 * Value Object: reprogramacion pendiente. La acepta solo la contraparte de quien la propuso.
 */
@Embeddable
public class PropuestaReprogramacion {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propuesta_franja_id")
    private FranjaHoraria nuevaFranja;
    @Column(name = "propuesta_por")
    private String propuestaPor;
    @Column(name = "propuesta_fecha")
    private LocalDateTime fecha;

    protected PropuestaReprogramacion() {
    }

    public PropuestaReprogramacion(FranjaHoraria nuevaFranja, String propuestaPor, LocalDateTime fecha) {
        this.nuevaFranja = nuevaFranja;
        this.propuestaPor = propuestaPor;
        this.fecha = fecha;
    }

    public boolean puedeAceptarla(String userId) {
        return !propuestaPor.equals(userId);
    }

    public FranjaHoraria getNuevaFranja() {
        return nuevaFranja;
    }

    public String getPropuestaPor() {
        return propuestaPor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
