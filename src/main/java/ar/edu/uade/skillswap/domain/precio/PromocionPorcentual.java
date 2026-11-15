package ar.edu.uade.skillswap.domain.precio;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("PORCENTUAL")
public class PromocionPorcentual extends Promocion {

    private int porcentaje;

    protected PromocionPorcentual() {
    }

    public PromocionPorcentual(String codigo, LocalDate vigenteDesde, LocalDate vigenteHasta, int porcentaje) {
        super(codigo, vigenteDesde, vigenteHasta);
        this.porcentaje = porcentaje;
    }

    @Override
    public ICalculoPrecio decorar(ICalculoPrecio precio) {
        return new DescuentoPorcentual(precio, porcentaje);
    }
}
