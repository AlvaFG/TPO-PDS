package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.precio.DescuentoMontoFijo;
import ar.edu.uade.skillswap.domain.precio.DescuentoPorcentual;
import ar.edu.uade.skillswap.domain.precio.ICalculoPrecio;
import ar.edu.uade.skillswap.domain.precio.PromocionMontoFijo;
import ar.edu.uade.skillswap.domain.precio.PromocionPorcentual;
import ar.edu.uade.skillswap.soporte.Datos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/* Decorator: promociones acumulables que nunca producen un total negativo (caso de aceptacion 9). */
class PromocionDecoratorTest {

    private Publicacion publicacion(long precio) {
        return Datos.borrador(Datos.profesional("prov"), new Categoria("Tec"), precio, 1, null, List.of(),
                Datos.INICIO);
    }

    @Test
    void promocionesAcumulables() {
        ICalculoPrecio precio = new DescuentoMontoFijo(new DescuentoPorcentual(publicacion(1000), 10), new SkillCoins(100));
        assertEquals(new SkillCoins(800), precio.calcularPrecio());
        assertEquals("Precio base 1000 SC - 10 % - 100 SC", precio.getDescripcion());
    }

    @Test
    void elTotalNuncaEsNegativo() {
        ICalculoPrecio precio = new DescuentoMontoFijo(new DescuentoPorcentual(publicacion(400), 30), new SkillCoins(500));
        assertEquals(SkillCoins.CERO, precio.calcularPrecio());
        ICalculoPrecio enOtroOrden = new DescuentoPorcentual(new DescuentoMontoFijo(publicacion(400), new SkillCoins(500)), 30);
        assertEquals(SkillCoins.CERO, enOtroOrden.calcularPrecio());
    }

    @Test
    void cotizarAplicaSoloLasPromocionesVigentes() {
        Publicacion p = publicacion(1000);
        p.agregarPromocion(new PromocionPorcentual("VERANO", Datos.HOY.minusDays(1), Datos.HOY.plusDays(1), 50));
        p.agregarPromocion(new PromocionMontoFijo("FIJO", Datos.HOY, Datos.HOY, new SkillCoins(100)));
        p.agregarPromocion(new PromocionMontoFijo("VENCIDA", Datos.HOY.minusDays(9), Datos.HOY.minusDays(2), new SkillCoins(999)));
        assertEquals(new SkillCoins(400), p.cotizar(Datos.HOY).calcularPrecio());
        assertEquals(new SkillCoins(1000), p.cotizar(Datos.HOY.plusDays(5)).calcularPrecio());
        assertEquals(3, p.getPromociones().size());
        assertEquals("VERANO", p.getPromociones().get(0).getCodigo());
    }

    @Test
    void datosInvalidos() {
        Publicacion p = publicacion(100);
        assertThrows(IllegalArgumentException.class, () -> new DescuentoPorcentual(p, 101));
        assertThrows(IllegalArgumentException.class, () -> new DescuentoPorcentual(null, 10));
        assertThrows(IllegalArgumentException.class, () -> new DescuentoMontoFijo(p, null));
        assertThrows(IllegalArgumentException.class,
                () -> new PromocionPorcentual("X", Datos.HOY, Datos.HOY.minusDays(1), 10));
    }
}
