package ar.edu.uade.skillswap.domain.model;

import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/*
 * Value Object: monto de creditos ficticios.
 * Es inmutable y nunca negativo: cada operacion devuelve una instancia nueva.
 */
@Embeddable
public class SkillCoins {

    public static final SkillCoins CERO = new SkillCoins(0);

    private long cantidad;

    protected SkillCoins() {
    }

    public SkillCoins(long cantidad) {
        if (cantidad < 0) {
            throw new ReglaNegocioException("Un monto de SkillCoins no puede ser negativo");
        }
        this.cantidad = cantidad;
    }

    public SkillCoins sumar(SkillCoins otro) {
        return new SkillCoins(cantidad + otro.cantidad);
    }

    /* Lanza ReglaNegocioException si el resultado fuera negativo. */
    public SkillCoins restar(SkillCoins otro) {
        if (esMenorQue(otro)) {
            throw new ReglaNegocioException("Saldo insuficiente: " + cantidad + " < " + otro.cantidad);
        }
        return new SkillCoins(cantidad - otro.cantidad);
    }

    /* Resta con piso en cero: la usan las promociones para no dar un total negativo. */
    public SkillCoins restarHastaCero(SkillCoins otro) {
        return new SkillCoins(Math.max(0, cantidad - otro.cantidad));
    }

    public SkillCoins porcentaje(int valor) {
        if (valor < 0 || valor > 100) {
            throw new ReglaNegocioException("El porcentaje debe estar entre 0 y 100");
        }
        return new SkillCoins(cantidad * valor / 100);
    }

    public boolean esMenorQue(SkillCoins otro) {
        return cantidad < otro.cantidad;
    }

    public long getCantidad() {
        return cantidad;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SkillCoins otro && cantidad == otro.cantidad;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cantidad);
    }

    @Override
    public String toString() {
        return cantidad + " SC";
    }
}
