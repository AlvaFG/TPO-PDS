package ar.edu.uade.skillswap.domain.precio;

import ar.edu.uade.skillswap.domain.model.SkillCoins;

/*
 * Decorator Pattern - Componente.
 * Lo implementan la publicacion (componente concreto) y cada promocion (decorador).
 */
public interface ICalculoPrecio {

    SkillCoins calcularPrecio();

    String getDescripcion();
}
