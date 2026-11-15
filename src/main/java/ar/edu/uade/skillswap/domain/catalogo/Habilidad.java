package ar.edu.uade.skillswap.domain.catalogo;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.List;

/*
 * Composite Pattern - Hoja.
 */
@Entity
@DiscriminatorValue("HABILIDAD")
public class Habilidad extends ElementoCatalogo {

    protected Habilidad() {
    }

    public Habilidad(String nombre) {
        super(nombre);
    }

    @Override
    public boolean contiene(ElementoCatalogo elemento) {
        return equals(elemento);
    }

    @Override
    public List<Habilidad> listarHabilidades() {
        return List.of(this);
    }
}
