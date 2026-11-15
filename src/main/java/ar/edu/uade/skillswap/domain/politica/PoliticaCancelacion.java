package ar.edu.uade.skillswap.domain.politica;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import java.time.Duration;
import java.time.LocalDateTime;

/*
 * Strategy Pattern - Estrategia.
 * Calcula cuanto se reintegra al cliente segun la anticipacion de la cancelacion.
 * La elige el autor de la publicacion; la ejecuta la contratacion al cancelar.
 * Persistencia: SINGLE_TABLE (politica_cancelacion) con discriminador tipo.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class PoliticaCancelacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombre;

    protected PoliticaCancelacion() {
    }

    protected PoliticaCancelacion(String nombre) {
        this.nombre = nombre;
    }

    public abstract SkillCoins calcularReintegro(SkillCoins pagado, LocalDateTime inicio, LocalDateTime cancelacion);

    /* Horas completas entre la cancelacion y el inicio de la sesion (negativo si ya empezo). */
    protected long horasDeAnticipacion(LocalDateTime inicio, LocalDateTime cancelacion) {
        return Duration.between(cancelacion, inicio).toHours();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
