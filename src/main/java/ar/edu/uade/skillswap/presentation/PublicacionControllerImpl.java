package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.dto.AccionPublicacion;
import ar.edu.uade.skillswap.application.dto.CambiarEstadoCommand;
import ar.edu.uade.skillswap.application.dto.CategoriaDto;
import ar.edu.uade.skillswap.application.dto.PublicacionCommand;
import ar.edu.uade.skillswap.application.service.IBusquedaService;
import ar.edu.uade.skillswap.application.service.IPublicacionService;
import ar.edu.uade.skillswap.controllers.UiControllers.PublicacionController;
import ar.edu.uade.skillswap.domain.model.TipoPublicacion;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PublicacionControllerImpl implements PublicacionController {

    /* La UI no carga horarios: el borrador nace con una franja por dia, desde la semana proxima, para poder publicarse. */
    private static final int FRANJAS_POR_DEFECTO = 3;
    private static final int DIAS_HASTA_PRIMERA_FRANJA = 7;

    private final IPublicacionService publicaciones;
    private final IBusquedaService busqueda;
    private final EjecutorDeAcciones ejecutor;
    private final Clock reloj;

    public PublicacionControllerImpl(IPublicacionService publicaciones, IBusquedaService busqueda,
                                     EjecutorDeAcciones ejecutor, Clock reloj) {
        this.publicaciones = publicaciones;
        this.busqueda = busqueda;
        this.ejecutor = ejecutor;
        this.reloj = reloj;
    }

    @Override
    public List<Publicacion> listarPropias(String userId) {
        return ejecutor.ejecutar(userId, () -> publicaciones.listarPropias(userId).stream()
                .map(Conversor::publicacion).toList());
    }

    @Override
    public Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria,
                                     Modalidad modalidad, int duracion, int precio) {
        return ejecutor.ejecutar(userId, () -> {
            PublicacionCommand cmd = new PublicacionCommand(TipoPublicacion.MENTORIA, titulo, descripcion,
                    categoriaId(categoria), List.of(), ModalidadUi.aDominio(modalidad), duracion,
                    FRANJAS_POR_DEFECTO, precio, Set.of(categoria), franjas(), null);
            Long id = publicaciones.crearBorrador(userId, cmd);
            return propia(userId, id);
        });
    }

    @Override
    public Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado) {
        return ejecutor.ejecutar(userId, () -> {
            Long id = Conversor.id(publicacionId);
            publicaciones.cambiarEstado(userId, new CambiarEstadoCommand(id, accion(userId, id, estado),
                    "Cambio de estado desde la aplicacion"));
            return propia(userId, id);
        });
    }

    /*
     * La UI pide el estado destino; el nucleo trabaja con acciones. PUBLICADA desde PAUSADA es
     * REANUDAR y desde BORRADOR es PUBLICAR.
     */
    private AccionPublicacion accion(String userId, Long id, EstadoPublicacion destino) {
        switch (destino) {
            case PUBLICADA:
                return propia(userId, id).estado() == EstadoPublicacion.PAUSADA
                        ? AccionPublicacion.REANUDAR : AccionPublicacion.PUBLICAR;
            case PAUSADA:
                return AccionPublicacion.PAUSAR;
            case CERRADA:
                return AccionPublicacion.CERRAR;
            case CANCELADA:
                return AccionPublicacion.CANCELAR;
            default:
                throw new ErrorDeAplicacion("No se puede pasar una publicacion a " + destino);
        }
    }

    private Publicacion propia(String userId, Long id) {
        return publicaciones.listarPropias(userId).stream().filter(p -> p.id().equals(id)).findFirst()
                .map(Conversor::publicacion)
                .orElseThrow(() -> new ErrorDeAplicacion("No existe la publicacion " + id));
    }

    private Long categoriaId(String nombre) {
        return busqueda.listarCategorias().stream().filter(c -> c.nombre().equals(nombre)).findFirst()
                .map(CategoriaDto::id)
                .orElseThrow(() -> new ErrorDeAplicacion("No existe la categoria " + nombre));
    }

    private List<LocalDateTime> franjas() {
        LocalDateTime primera = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.HOURS).plusDays(DIAS_HASTA_PRIMERA_FRANJA);
        List<LocalDateTime> inicios = new ArrayList<>();
        for (int i = 0; i < FRANJAS_POR_DEFECTO; i++) {
            inicios.add(primera.plusDays(i));
        }
        return inicios;
    }
}
