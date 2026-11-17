package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.PerfilCommand;
import ar.edu.uade.skillswap.application.dto.PerfilPublicoDto;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.IBilleteraRepository;
import ar.edu.uade.skillswap.application.port.ICatalogoRepository;
import ar.edu.uade.skillswap.application.port.IContratacionRepository;
import ar.edu.uade.skillswap.application.port.IGestorTransacciones;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.exception.ReglaNegocioException;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.SkillCoins;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

public class PerfilService implements IPerfilService {

    private static final int TAMANIO_HISTORIAL = 10;

    private final IPerfilRepository perfilRepository;
    private final IPublicacionRepository publicacionRepository;
    private final IContratacionRepository contratacionRepository;
    private final ICatalogoRepository catalogoRepository;
    private final IBilleteraRepository billeteraRepository;
    private final IGestorTransacciones transacciones;

    public PerfilService(IPerfilRepository perfilRepository, IPublicacionRepository publicacionRepository,
                         IContratacionRepository contratacionRepository, ICatalogoRepository catalogoRepository,
                         IBilleteraRepository billeteraRepository, IGestorTransacciones transacciones) {
        this.perfilRepository = perfilRepository;
        this.publicacionRepository = publicacionRepository;
        this.contratacionRepository = contratacionRepository;
        this.catalogoRepository = catalogoRepository;
        this.billeteraRepository = billeteraRepository;
        this.transacciones = transacciones;
    }

    /* Crea el perfil y su billetera (saldo 0) en la misma transaccion. */
    @Override
    public PerfilPublicoDto crearPerfil(String userId, PerfilCommand cmd) {
        Perfil perfil = transacciones.ejecutar(() -> {
            if (perfilRepository.buscarPorUserId(userId).isPresent()) {
                throw new ReglaNegocioException("El usuario ya tiene un perfil");
            }
            Perfil nuevo = new Perfil(userId, cmd.tipo(), cmd.nombreVisible());
            aplicarDatos(nuevo, cmd);
            Perfil guardado = perfilRepository.guardar(nuevo);
            billeteraRepository.guardar(new Billetera(guardado, SkillCoins.CERO));
            return guardado;
        });
        return armarDto(perfil);
    }

    @Override
    public void actualizarPerfil(String userId, PerfilCommand cmd) {
        transacciones.ejecutar(() -> {
            Perfil perfil = buscarPorUserId(userId);
            aplicarDatos(perfil, cmd);
            return perfilRepository.guardar(perfil);
        });
    }

    @Override
    public PerfilPublicoDto verPerfil(Long perfilId) {
        Perfil perfil = perfilRepository.buscarPorId(perfilId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el perfil " + perfilId));
        return armarDto(perfil);
    }

    @Override
    public PerfilPublicoDto verPerfilDeUsuario(String userId) {
        return armarDto(buscarPorUserId(userId));
    }

    @Override
    public void actualizarBasico(String userId, String nombreVisible, String biografia) {
        transacciones.ejecutar(() -> {
            Perfil perfil = buscarPorUserId(userId);
            perfil.actualizarDatos(nombreVisible, biografia, perfil.getZonaHoraria());
            return perfilRepository.guardar(perfil);
        });
    }

    /* Baja logica: las operaciones historicas se conservan. */
    @Override
    public void darDeBaja(String userId) {
        transacciones.ejecutar(() -> {
            Perfil perfil = buscarPorUserId(userId);
            perfil.darDeBaja();
            return perfilRepository.guardar(perfil);
        });
    }

    private void aplicarDatos(Perfil perfil, PerfilCommand cmd) {
        perfil.actualizarDatos(cmd.nombreVisible(), cmd.biografia(), zona(cmd.zonaHoraria()));
        perfil.definirHabilidades(habilidades(cmd.habilidadIds()), habilidades(cmd.interesIds()));
        perfil.definirDisponibilidad(cmd.disponibilidad() == null ? List.of() : cmd.disponibilidad());
        if (cmd.canales() != null && !cmd.canales().isEmpty()) {
            perfil.elegirCanales(cmd.canales());
        }
    }

    private List<Habilidad> habilidades(List<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : catalogoRepository.buscarHabilidades(ids);
    }

    private ZoneId zona(String zonaHoraria) {
        if (zonaHoraria == null || zonaHoraria.isBlank()) {
            return null;
        }
        try {
            return ZoneId.of(zonaHoraria);
        } catch (DateTimeException e) {
            throw new ReglaNegocioException("Zona horaria invalida: " + zonaHoraria);
        }
    }

    private Perfil buscarPorUserId(String userId) {
        return perfilRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no tiene perfil"));
    }

    private PerfilPublicoDto armarDto(Perfil perfil) {
        List<PublicacionDto> activas = publicacionRepository.listarActivasDe(perfil.getId()).stream()
                .map(PublicacionDto::desde).toList();
        List<String> historial = contratacionRepository.listarPorParticipante(perfil.getUserId(), 0, TAMANIO_HISTORIAL)
                .contenido().stream()
                .map(c -> c.getPublicacion().getTitulo() + " - " + c.getNombreEstado())
                .toList();
        List<String> habilidades = perfil.getHabilidades().stream().map(Habilidad::getNombre).toList();
        return new PerfilPublicoDto(perfil.getId(), perfil.getTipo().name(), perfil.getNombreVisible(), perfil.getBiografia(),
                perfil.getReputacion(), perfil.getCantidadCalificaciones(), habilidades, activas, historial);
    }
}
