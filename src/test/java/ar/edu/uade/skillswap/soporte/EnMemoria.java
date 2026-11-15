package ar.edu.uade.skillswap.soporte;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Almacen en memoria que asigna ids incrementales al guardar.
 */
public class EnMemoria<T> {

    private final Map<Long, T> datos = new LinkedHashMap<>();
    private long secuencia = 1;

    public T guardar(T entidad) {
        Long id = Ids.leer(entidad);
        if (id == null) {
            id = secuencia++;
            Ids.asignar(entidad, id);
        }
        datos.put(id, entidad);
        return entidad;
    }

    public Optional<T> buscar(Long id) {
        return Optional.ofNullable(datos.get(id));
    }

    public List<T> todos() {
        return new ArrayList<>(datos.values());
    }
}
