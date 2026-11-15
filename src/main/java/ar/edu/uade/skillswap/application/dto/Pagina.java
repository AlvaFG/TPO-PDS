package ar.edu.uade.skillswap.application.dto;

import java.util.List;
import java.util.function.Function;

/*
 * Pagina de resultados: todos los listados de consulta se devuelven paginados.
 * numero empieza en 0.
 */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long totalElementos) {

    public Pagina {
        if (numero < 0 || tamanio <= 0) {
            throw new IllegalArgumentException("Pagina invalida: numero >= 0 y tamanio > 0");
        }
        contenido = List.copyOf(contenido);
    }

    /* Recorta una lista completa en la pagina pedida. */
    public static <T> Pagina<T> de(List<T> todos, int numero, int tamanio) {
        int desde = (int) Math.max(0, Math.min((long) numero * tamanio, todos.size()));
        int hasta = Math.min(desde + tamanio, todos.size());
        return new Pagina<>(todos.subList(desde, hasta), numero, tamanio, todos.size());
    }

    public int totalPaginas() {
        return (int) ((totalElementos + tamanio - 1) / tamanio);
    }

    public <R> Pagina<R> map(Function<T, R> conversion) {
        return new Pagina<>(contenido.stream().map(conversion).toList(), numero, tamanio, totalElementos);
    }
}
