package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/*
 * Turno concreto que ofrece una publicacion. Se reserva para una sola contratacion.
 */
@Entity
public class FranjaHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime inicio;
    @Column(nullable = false)
    private LocalDateTime fin;
    private boolean ocupada;

    protected FranjaHoraria() {
    }

    public FranjaHoraria(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new ReglaNegocioException("La franja debe terminar despues de empezar");
        }
        this.inicio = inicio;
        this.fin = fin;
    }

    public void ocupar() {
        if (ocupada) {
            throw new ReglaNegocioException("La franja ya esta ocupada");
        }
        ocupada = true;
    }

    public void liberar() {
        ocupada = false;
    }

    public boolean seSuperponeCon(FranjaHoraria otra) {
        return inicio.isBefore(otra.fin) && otra.inicio.isBefore(fin);
    }

    public boolean estaLibre() {
        return !ocupada;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }
}
