package ar.edu.uade.skillswap.domain.catalogo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Composite Pattern - Compuesto.
 * Agrupa subcategorias y habilidades, y delega en sus hijos.
 */
@Entity
@DiscriminatorValue("CATEGORIA")
public class Categoria extends ElementoCatalogo {

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "padre_id")
    @OrderBy("id")
    private List<ElementoCatalogo> hijos = new ArrayList<>();

    protected Categoria() {
    }

    public Categoria(String nombre) {
        super(nombre);
    }

    public void agregar(ElementoCatalogo elemento) {
        if (elemento == null || elemento.contiene(this)) {
            throw new IllegalArgumentException("No se puede agregar el elemento: generaria un ciclo");
        }
        hijos.add(elemento);
    }

    public void quitar(ElementoCatalogo elemento) {
        hijos.remove(elemento);
    }

    @Override
    public boolean contiene(ElementoCatalogo elemento) {
        return equals(elemento) || hijos.stream().anyMatch(hijo -> hijo.contiene(elemento));
    }

    @Override
    public List<Habilidad> listarHabilidades() {
        List<Habilidad> habilidades = new ArrayList<>();
        hijos.forEach(hijo -> habilidades.addAll(hijo.listarHabilidades()));
        return habilidades;
    }

    /* Ids de esta categoria y de todas sus subcategorias: lo usa la busqueda por categoria. */
    public List<Long> listarSubcategoriaIds() {
        List<Long> ids = new ArrayList<>();
        recolectarCategoriaIds(ids);
        return ids;
    }

    @Override
    protected void recolectarCategoriaIds(List<Long> ids) {
        ids.add(getId());
        hijos.forEach(hijo -> hijo.recolectarCategoriaIds(ids));
    }

    public List<ElementoCatalogo> getHijos() {
        return Collections.unmodifiableList(hijos);
    }
}
