package ar.edu.uade.skillswap.domain.precio;

import ar.edu.uade.skillswap.domain.model.SkillCoins;

/*
 * Decorador concreto: descuenta un monto fijo, con piso en cero.
 */
public class DescuentoMontoFijo extends PromocionDecorator {

    private final SkillCoins monto;

    public DescuentoMontoFijo(ICalculoPrecio envuelto, SkillCoins monto) {
        super(envuelto);
        if (monto == null) {
            throw new IllegalArgumentException("El monto del descuento es obligatorio");
        }
        this.monto = monto;
    }

    @Override
    public SkillCoins aplicar(SkillCoins precio) {
        return precio.restarHastaCero(monto);
    }

    @Override
    public String getDescripcion() {
        return envuelto.getDescripcion() + " - " + monto;
    }
}
