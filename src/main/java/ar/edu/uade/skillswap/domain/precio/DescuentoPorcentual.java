package ar.edu.uade.skillswap.domain.precio;

import ar.edu.uade.skillswap.domain.model.SkillCoins;

/*
 * Decorador concreto: descuenta un porcentaje del precio que recibe.
 */
public class DescuentoPorcentual extends PromocionDecorator {

    private final int porcentaje;

    public DescuentoPorcentual(ICalculoPrecio envuelto, int porcentaje) {
        super(envuelto);
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        this.porcentaje = porcentaje;
    }

    @Override
    public SkillCoins aplicar(SkillCoins precio) {
        return precio.restarHastaCero(precio.porcentaje(porcentaje));
    }

    @Override
    public String getDescripcion() {
        return envuelto.getDescripcion() + " - " + porcentaje + " %";
    }
}
