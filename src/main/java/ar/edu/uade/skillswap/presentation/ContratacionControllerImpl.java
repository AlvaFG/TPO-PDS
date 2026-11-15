package ar.edu.uade.skillswap.presentation;

import ar.edu.uade.skillswap.application.dto.ContratacionDto;
import ar.edu.uade.skillswap.application.dto.SolicitarCommand;
import ar.edu.uade.skillswap.application.service.IContratacionService;
import ar.edu.uade.skillswap.controllers.UiControllers.ContratacionController;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.model.Models.Contratacion;

import java.util.List;

public class ContratacionControllerImpl implements ContratacionController {

    /* La UI no pagina: se pide una unica pagina lo bastante grande. */
    private static final int TAMANIO_PAGINA = 1000;

    private final IContratacionService contrataciones;
    private final EjecutorDeAcciones ejecutor;

    public ContratacionControllerImpl(IContratacionService contrataciones, EjecutorDeAcciones ejecutor) {
        this.contrataciones = contrataciones;
        this.ejecutor = ejecutor;
    }

    @Override
    public List<Contratacion> listarContrataciones(String userId) {
        return ejecutor.ejecutar(userId, () -> contrataciones.listar(userId, 0, TAMANIO_PAGINA)
                .contenido().stream().map(Conversor::contratacion).toList());
    }

    /* La UI no elige franja: se toma la primera libre de la publicacion (franjaId null). */
    @Override
    public Contratacion solicitar(String userId, String publicacionId, String objetivo) {
        return ejecutor.ejecutar(userId, () -> {
            Long id = contrataciones.solicitar(userId,
                    new SolicitarCommand(Conversor.id(publicacionId), null, null, objetivo));
            return ver(userId, id);
        });
    }

    /*
     * Un solo boton para todo el flujo: segun el estado y el rol elige el caso de uso.
     * PENDIENTE: aceptar (proveedor); ACEPTADA: pagar (cliente); CONFIRMADA: iniciar (proveedor);
     * EN_CURSO: completar (proveedor). Cualquier otra combinacion no tiene accion valida.
     */
    @Override
    public Contratacion avanzar(String userId, String contratacionId) {
        return ejecutor.ejecutar(userId, () -> {
            Long id = Conversor.id(contratacionId);
            ContratacionDto actual = contrataciones.verComprobante(userId, id);
            boolean proveedor = ContratacionDto.PROVEEDOR.equals(actual.rol());
            if (proveedor && "PENDIENTE".equals(actual.estado())) {
                contrataciones.aceptar(userId, id);
            } else if (!proveedor && "ACEPTADA".equals(actual.estado())) {
                contrataciones.pagar(userId, id);
            } else if (proveedor && "CONFIRMADA".equals(actual.estado())) {
                contrataciones.iniciar(userId, id);
            } else if (proveedor && "EN_CURSO".equals(actual.estado())) {
                contrataciones.completar(userId, id);
            } else {
                throw new ReglaNegocioException("No hay una accion disponible para el " + actual.rol().toLowerCase()
                        + " con la contratacion en estado " + actual.estado());
            }
            return ver(userId, id);
        });
    }

    @Override
    public Contratacion cancelar(String userId, String contratacionId) {
        return ejecutor.ejecutar(userId, () -> {
            Long id = Conversor.id(contratacionId);
            contrataciones.cancelar(userId, id, "Cancelada desde la aplicacion");
            return ver(userId, id);
        });
    }

    private Contratacion ver(String userId, Long id) {
        return Conversor.contratacion(contrataciones.verComprobante(userId, id));
    }
}
