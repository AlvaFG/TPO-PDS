package ar.edu.uade.skillswap.domain.precio;

import ar.edu.uade.skillswap.domain.model.SkillCoins;

/*
 * Decorator Pattern - Decorador base.
 * Envuelve otro calculo de precio y le aplica su ajuste.
 * Como SkillCoins nunca es negativo, ninguna cadena produce un total negativo.
 */
public abstract class PromocionDecorator implements ICalculoPrecio {

    protected final ICalculoPrecio envuelto;

    protected PromocionDecorator(ICalculoPrecio envuelto) {
        if (envuelto == null) {
            throw new IllegalArgumentException("El precio a decorar es obligatorio");
        }
        this.envuelto = envuelto;
    }

    @Override
    public SkillCoins calcularPrecio() {
        return aplicar(envuelto.calcularPrecio());
    }

    @Override
    public String getDescripcion() {
        return envuelto.getDescripcion();
    }

    public abstract SkillCoins aplicar(SkillCoins precio);
}
