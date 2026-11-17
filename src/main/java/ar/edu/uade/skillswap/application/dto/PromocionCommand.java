package ar.edu.uade.skillswap.application.dto;

/*
 * Promocion a agregar a una publicacion. Vale desde hoy durante diasVigencia dias.
 * valor es el porcentaje (PORCENTUAL) o la cantidad de SkillCoins a descontar (MONTO_FIJO).
 */
public record PromocionCommand(Long publicacionId, Tipo tipo, int valor, String codigo, int diasVigencia) {

    public enum Tipo { PORCENTUAL, MONTO_FIJO }
}
