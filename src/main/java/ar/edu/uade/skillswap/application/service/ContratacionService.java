package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.ContratacionDto;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.dto.SolicitarCommand;
import ar.edu.uade.skillswap.application.evento.IPublicadorEventos;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.IBilleteraRepository;
import ar.edu.uade.skillswap.application.port.IContratacionRepository;
import ar.edu.uade.skillswap.application.port.IGestorTransacciones;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Contratacion;
import ar.edu.uade.skillswap.domain.model.EventoContratacion;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoEvento;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * Casos de uso de la contratacion. Cada comando corre dentro de IGestorTransacciones
 * y el evento se publica despues de confirmar, para no notificar operaciones con rollback.
 */
public class ContratacionService implements IContratacionService {

    private final IContratacionRepository contratacionRepository;
    private final IPublicacionRepository publicacionRepository;
    private final IPerfilRepository perfilRepository;
    private final IBilleteraRepository billeteraRepository;
    private final IGestorTransacciones transacciones;
    private final IPublicadorEventos eventos;
    private final Clock reloj;

    public ContratacionService(IContratacionRepository contratacionRepository,
                               IPublicacionRepository publicacionRepository, IPerfilRepository perfilRepository,
                               IBilleteraRepository billeteraRepository, IGestorTransacciones transacciones,
                               IPublicadorEventos eventos, Clock reloj) {
        this.contratacionRepository = contratacionRepository;
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.billeteraRepository = billeteraRepository;
        this.transacciones = transacciones;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public Long solicitar(String userId, SolicitarCommand cmd) {
        Contratacion contratacion = transacciones.ejecutar(() -> {
            Perfil cliente = perfilRepository.buscarPorUserId(userId)
                    .filter(Perfil::isActivo)
                    .orElseThrow(() -> new AccesoDenegadoException("El usuario no tiene un perfil activo"));
            Publicacion publicacion = publicacionRepository.buscarPorId(cmd.publicacionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la publicacion " + cmd.publicacionId()));
            FranjaHoraria franja = franjaDe(publicacion, cmd.franjaId());
            return contratacionRepository.guardar(new Contratacion(publicacion, cliente, franja,
                    cmd.mensaje(), cmd.objetivo(), LocalDate.now(reloj)));
        });
        eventos.publicar(EventoContratacion.de(contratacion, TipoEvento.SOLICITUD_RECIBIDA));
        return contratacion.getId();
    }

    /* Reserva franja y cupo en la misma transaccion (la version de Publicacion evita la sobreventa). */
    @Override
    public void aceptar(String userId, Long contratacionId) {
        Contratacion contratacion = transacciones.ejecutar(() -> {
            Contratacion c = delProveedor(userId, contratacionId);
            c.aceptar(userId);
            publicacionRepository.guardar(c.getPublicacion());
            return contratacionRepository.guardar(c);
        });
        eventos.publicar(EventoContratacion.de(contratacion, TipoEvento.SOLICITUD_ACEPTADA));
    }

    @Override
    public void rechazar(String userId, Long contratacionId, String motivo) {
        transacciones.ejecutar(() -> {
            Contratacion c = delProveedor(userId, contratacionId);
            c.rechazar(userId, motivo);
            return contratacionRepository.guardar(c);
        });
    }

    /* Debito al cliente y credito al proveedor en una unica transaccion. */
    @Override
    public void pagar(String userId, Long contratacionId) {
        Contratacion contratacion = transacciones.ejecutar(() -> {
            Contratacion c = buscar(contratacionId);
            if (!c.esCliente(userId)) {
                throw new AccesoDenegadoException("Solo el cliente puede pagar la contratacion");
            }
            Billetera cliente = billeteraDe(c.getCliente());
            Billetera proveedor = billeteraDe(c.getProveedor());
            c.pagar(userId, cliente, proveedor);
            billeteraRepository.guardar(cliente);
            billeteraRepository.guardar(proveedor);
            return contratacionRepository.guardar(c);
        });
        eventos.publicar(EventoContratacion.de(contratacion, TipoEvento.PAGO_CONFIRMADO));
    }

    @Override
    public void proponerReprogramacion(String userId, Long contratacionId, Long franjaId) {
        Contratacion contratacion = transacciones.ejecutar(() -> {
            Contratacion c = deParticipante(userId, contratacionId);
            c.proponerReprogramacion(userId, franjaDe(c.getPublicacion(), franjaId));
            return contratacionRepository.guardar(c);
        });
        eventos.publicar(EventoContratacion.de(contratacion, TipoEvento.REPROGRAMACION));
    }

    @Override
    public void aceptarReprogramacion(String userId, Long contratacionId) {
        Contratacion contratacion = transacciones.ejecutar(() -> {
            Contratacion c = deParticipante(userId, contratacionId);
            c.aceptarReprogramacion(userId);
            return contratacionRepository.guardar(c);
        });
        eventos.publicar(EventoContratacion.de(contratacion, TipoEvento.REPROGRAMACION));
    }

    @Override
    public void iniciar(String userId, Long contratacionId) {
        transacciones.ejecutar(() -> {
            Contratacion c = delProveedor(userId, contratacionId);
            c.iniciar(userId);
            return contratacionRepository.guardar(c);
        });
    }

    @Override
    public void completar(String userId, Long contratacionId) {
        Contratacion contratacion = transacciones.ejecutar(() -> {
            Contratacion c = delProveedor(userId, contratacionId);
            c.completar(userId);
            return contratacionRepository.guardar(c);
        });
        eventos.publicar(EventoContratacion.de(contratacion, TipoEvento.SESION_COMPLETADA));
    }

    /* Reintegro, movimiento de billeteras y liberacion del cupo en una unica transaccion. */
    @Override
    public long cancelar(String userId, Long contratacionId, String motivo) {
        Cancelacion resultado = transacciones.ejecutar(() -> {
            Contratacion c = deParticipante(userId, contratacionId);
            Billetera cliente = billeteraDe(c.getCliente());
            Billetera proveedor = billeteraDe(c.getProveedor());
            SkillCoins reintegro = c.cancelar(userId, motivo, LocalDateTime.now(reloj), cliente, proveedor);
            billeteraRepository.guardar(cliente);
            billeteraRepository.guardar(proveedor);
            publicacionRepository.guardar(c.getPublicacion());
            return new Cancelacion(contratacionRepository.guardar(c), reintegro);
        });
        eventos.publicar(EventoContratacion.de(resultado.contratacion(), TipoEvento.CANCELACION));
        return resultado.reintegro().getCantidad();
    }

    /* Resultado de la transaccion de cancelar: la contratacion para el evento y el monto reintegrado. */
    private record Cancelacion(Contratacion contratacion, SkillCoins reintegro) {
    }

    @Override
    public Pagina<ContratacionDto> listar(String userId, int pagina, int tamanio) {
        return contratacionRepository.listarPorParticipante(userId, pagina, tamanio).map(c -> ContratacionDto.desde(c, userId));
    }

    @Override
    public ContratacionDto verComprobante(String userId, Long contratacionId) {
        return ContratacionDto.desde(deParticipante(userId, contratacionId), userId);
    }

    private Contratacion buscar(Long contratacionId) {
        return contratacionRepository.buscarPorId(contratacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la contratacion " + contratacionId));
    }

    private Contratacion deParticipante(String userId, Long contratacionId) {
        Contratacion c = buscar(contratacionId);
        if (!c.esParticipante(userId)) {
            throw new AccesoDenegadoException("El usuario no participa de la contratacion");
        }
        return c;
    }

    private Contratacion delProveedor(String userId, Long contratacionId) {
        Contratacion c = buscar(contratacionId);
        if (!c.esProveedor(userId)) {
            throw new AccesoDenegadoException("Solo el proveedor puede realizar esta accion");
        }
        return c;
    }

    private Billetera billeteraDe(Perfil perfil) {
        return billeteraRepository.buscarPorUserId(perfil.getUserId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la billetera de " + perfil.getUserId()));
    }

    /* Con franjaId null se toma la primera franja libre de la publicacion. */
    private FranjaHoraria franjaDe(Publicacion publicacion, Long franjaId) {
        if (franjaId == null) {
            return publicacion.getFranjas().stream()
                    .filter(FranjaHoraria::estaLibre)
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("La publicacion no tiene franjas libres"));
        }
        return publicacion.getFranjas().stream()
                .filter(f -> f.getId() != null && f.getId().equals(franjaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La publicacion no tiene la franja " + franjaId));
    }
}
