package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/*
 * Value Object: disponibilidad semanal del perfil (por ejemplo, lunes de 9 a 12).
 */
@Embeddable
public class RangoHorario {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dia;
    @Column(nullable = false)
    private LocalTime desde;
    @Column(nullable = false)
    private LocalTime hasta;

    protected RangoHorario() {
    }

    public RangoHorario(DayOfWeek dia, LocalTime desde, LocalTime hasta) {
        if (dia == null || desde == null || hasta == null || !desde.isBefore(hasta)) {
            throw new ReglaNegocioException("Rango horario invalido");
        }
        this.dia = dia;
        this.desde = desde;
        this.hasta = hasta;
    }

    public boolean incluye(LocalDateTime momento) {
        LocalTime hora = momento.toLocalTime();
        return momento.getDayOfWeek() == dia && !hora.isBefore(desde) && hora.isBefore(hasta);
    }

    public DayOfWeek getDia() {
        return dia;
    }

    public LocalTime getDesde() {
        return desde;
    }

    public LocalTime getHasta() {
        return hasta;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RangoHorario otro
                && dia == otro.dia && desde.equals(otro.desde) && hasta.equals(otro.hasta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dia, desde, hasta);
    }
}
