package ar.edu.uade.skillswap.soporte;

import java.lang.reflect.Field;

/*
 * Asigna el id como lo haria la base de datos. Solo para los repositorios en memoria de las pruebas.
 */
public final class Ids {

    private Ids() {
    }

    public static <T> T asignar(T entidad, Long id) {
        try {
            Field campo = campoId(entidad.getClass());
            campo.setAccessible(true);
            campo.set(entidad, id);
            return entidad;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Long leer(Object entidad) {
        try {
            Field campo = campoId(entidad.getClass());
            campo.setAccessible(true);
            return (Long) campo.get(entidad);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field campoId(Class<?> clase) {
        for (Class<?> c = clase; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField("id");
            } catch (NoSuchFieldException e) {
                // sigue con la superclase
            }
        }
        throw new IllegalArgumentException("La clase no tiene campo id: " + clase);
    }
}
