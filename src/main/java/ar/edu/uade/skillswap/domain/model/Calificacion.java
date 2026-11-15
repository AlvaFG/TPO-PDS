package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/*
 * Calificacion verificada: solo la crea una Contratacion COMPLETADA (ver Contratacion.calificar).
 * En la base, UNIQUE(contratacion_id, autor_id): cada participante califica una sola vez.
 */
@Entity
public class Calificacion {

    public static final int PUNTAJE_MINIMO = 1;
    public static final int PUNTAJE_MAXIMO = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacion_id")
    private Contratacion contratacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Perfil autor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calificado_id")
    private Perfil calificado;
    private int puntaje;
    private String comentario;
    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Calificacion() {
    }

    public Calificacion(Contratacion contratacion, Perfil autor, Perfil calificado,
                        int puntaje, String comentario, LocalDateTime fecha) {
        if (autor.getUserId().equals(calificado.getUserId())) {
            throw new ReglaNegocioException("Una persona no puede calificarse a si misma");
        }
        this.contratacion = contratacion;
        this.autor = autor;
        this.calificado = calificado;
        this.puntaje = puntaje;
        this.comentario = comentario;
        this.fecha = fecha;
        validarPuntaje();
    }

    public void validarPuntaje() {
        if (puntaje < PUNTAJE_MINIMO || puntaje > PUNTAJE_MAXIMO) {
            throw new ReglaNegocioException("El puntaje debe estar entre " + PUNTAJE_MINIMO + " y " + PUNTAJE_MAXIMO);
        }
    }

    public Long getId() {
        return id;
    }

    public Contratacion getContratacion() {
        return contratacion;
    }

    public Perfil getAutor() {
        return autor;
    }

    public Perfil getCalificado() {
        return calificado;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
