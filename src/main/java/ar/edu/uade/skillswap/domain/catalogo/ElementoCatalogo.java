package ar.edu.uade.skillswap.domain.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import java.util.List;

/*
 * Composite Pattern - Componente.
 * El cliente trata igual a una categoria (compuesto) que a una habilidad (hoja).
 * Persistencia: SINGLE_TABLE (elemento_catalogo) con discriminador tipo y autorreferencia padre_id.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class ElementoCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombre;

    protected ElementoCatalogo() {
    }

    protected ElementoCatalogo(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del elemento de catalogo es obligatorio");
        }
        this.nombre = nombre;
    }

    /* Indica si el elemento esta en este subarbol (incluido el propio elemento). */
    public abstract boolean contiene(ElementoCatalogo elemento);

    public abstract List<Habilidad> listarHabilidades();

    /* Por defecto no aporta ids: solo las categorias lo redefinen. */
    protected void recolectarCategoriaIds(List<Long> ids) {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    /* Dos elementos son el mismo si son la misma instancia o tienen el mismo id persistido. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ElementoCatalogo otro && getClass() == otro.getClass()
                && id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
