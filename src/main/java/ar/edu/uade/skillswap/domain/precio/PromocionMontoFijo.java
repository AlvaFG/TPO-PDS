package ar.edu.uade.skillswap.domain.precio;

import ar.edu.uade.skillswap.domain.model.SkillCoins;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("MONTO_FIJO")
public class PromocionMontoFijo extends Promocion {

    @Embedded
    @AttributeOverride(name = "cantidad", column = @Column(name = "monto"))
    private SkillCoins monto;

    protected PromocionMontoFijo() {
    }

    public PromocionMontoFijo(String codigo, LocalDate vigenteDesde, LocalDate vigenteHasta, SkillCoins monto) {
        super(codigo, vigenteDesde, vigenteHasta);
        this.monto = monto;
    }

    @Override
    public ICalculoPrecio decorar(ICalculoPrecio precio) {
        return new DescuentoMontoFijo(precio, monto);
    }
}
