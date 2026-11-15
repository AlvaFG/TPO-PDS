package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.IServicioIdentidad;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;

import java.util.function.Supplier;

/*
 * Pure Fabrication: valida el userId contra identidad, ejecuta la accion y traduce las excepciones
 * de negocio a ErrorDeAplicacion. Asi cada metodo de un controller es una sola linea.
 */
public class EjecutorDeAcciones {

    private final IServicioIdentidad identidad;

    public EjecutorDeAcciones(IServicioIdentidad identidad) {
        this.identidad = identidad;
    }

    public <T> T ejecutar(String userId, Supplier<T> accion) {
        try {
            if (userId == null || !identidad.validarUsuario(userId)) {
                throw new AccesoDenegadoException("El usuario no es valido: " + userId);
            }
            return accion.get();
        } catch (ReglaNegocioException | AccesoDenegadoException | RecursoNoEncontradoException e) {
            throw new ErrorDeAplicacion(e.getMessage());
        } catch (ConflictoConcurrenciaException e) {
            throw new ErrorDeAplicacion("Cupo agotado o publicacion modificada por otra operacion, reintente.");
        }
    }

    public void ejecutar(String userId, Runnable accion) {
        ejecutar(userId, () -> {
            accion.run();
            return null;
        });
    }
}
