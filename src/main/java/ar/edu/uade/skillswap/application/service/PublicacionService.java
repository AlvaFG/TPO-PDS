package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.dto.CambiarEstadoCommand;
import ar.edu.uade.skillswap.application.dto.PublicacionCommand;
import ar.edu.uade.skillswap.application.dto.PublicacionDto;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.port.ICatalogoRepository;
import ar.edu.uade.skillswap.application.port.IGestorTransacciones;
import ar.edu.uade.skillswap.application.port.IPerfilRepository;
import ar.edu.uade.skillswap.application.port.IPoliticaCancelacionRepository;
import ar.edu.uade.skillswap.application.port.IPublicacionRepository;
import ar.edu.uade.skillswap.domain.catalogo.Categoria;
import ar.edu.uade.skillswap.domain.catalogo.Habilidad;
import ar.edu.uade.skillswap.domain.model.FranjaHoraria;
import ar.edu.uade.skillswap.domain.model.Perfil;
import ar.edu.uade.skillswap.domain.model.Publicacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.politica.PoliticaCancelacion;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class PublicacionService implements IPublicacionService {

    private final IPublicacionRepository publicacionRepository;
    private final IPerfilRepository perfilRepository;
    private final ICatalogoRepository catalogoRepository;
    private final IPoliticaCancelacionRepository politicaRepository;
    private final IGestorTransacciones transacciones;
    private final Clock reloj;

    public PublicacionService(IPublicacionRepository publicacionRepository, IPerfilRepository perfilRepository,
                              ICatalogoRepository catalogoRepository, IPoliticaCancelacionRepository politicaRepository,
                              IGestorTransacciones transacciones, Clock reloj) {
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.catalogoRepository = catalogoRepository;
        this.politicaRepository = politicaRepository;
        this.transacciones = transacciones;
        this.reloj = reloj;
    }

    @Override
    public Long crearBorrador(String userId, PublicacionCommand cmd) {
        return transacciones.ejecutar(() -> {
            Perfil autor = perfilActivo(userId);
            Publicacion publicacion = new Publicacion(autor, cmd.tipo(), cmd.titulo(), cmd.descripcion(),
                    categoria(cmd.categoriaId()), habilidades(cmd.habilidadIds()), cmd.modalidad(),
                    cmd.duracionMinutos(), cmd.cupo(), new SkillCoins(cmd.precioBase()),
                    cmd.etiquetas() == null ? Set.of() : cmd.etiquetas(), politica(cmd.politicaCancelacionId()));
            List<LocalDateTime> inicios = cmd.inicioFranjas() == null ? List.of() : cmd.inicioFranjas();
            for (LocalDateTime inicio : inicios) {
                publicacion.agregarFranja(new FranjaHoraria(inicio, inicio.plusMinutes(cmd.duracionMinutos())));
            }
            return publicacionRepository.guardar(publicacion).getId();
        });
    }

    /* Solo en BORRADOR (lo valida el dominio). Cambia titulo, descripcion y precio base. */
    @Override
    public void editar(String userId, Long publicacionId, PublicacionCommand cmd) {
        transacciones.ejecutar(() -> {
            Publicacion publicacion = publicacionDelAutor(userId, publicacionId);
            publicacion.editar(cmd.titulo(), cmd.descripcion(), new SkillCoins(cmd.precioBase()));
            return publicacionRepository.guardar(publicacion);
        });
    }

    @Override
    public void cambiarEstado(String userId, CambiarEstadoCommand cmd) {
        transacciones.ejecutar(() -> {
            Publicacion publicacion = publicacionDelAutor(userId, cmd.publicacionId());
            cmd.accion().ejecutar(publicacion, userId, cmd.motivo(), LocalDateTime.now(reloj));
            return publicacionRepository.guardar(publicacion);
        });
    }

    @Override
    public List<PublicacionDto> listarPropias(String userId) {
        Perfil autor = perfilActivo(userId);
        return publicacionRepository.listarDeAutor(autor.getId()).stream().map(PublicacionDto::desde).toList();
    }

    private Publicacion publicacionDelAutor(String userId, Long publicacionId) {
        Publicacion publicacion = publicacionRepository.buscarPorId(publicacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la publicacion " + publicacionId));
        if (!publicacion.esAutor(userId)) {
            throw new AccesoDenegadoException("Solo el autor puede modificar la publicacion");
        }
        return publicacion;
    }

    private Perfil perfilActivo(String userId) {
        return perfilRepository.buscarPorUserId(userId)
                .filter(Perfil::isActivo)
                .orElseThrow(() -> new AccesoDenegadoException("El usuario no tiene un perfil activo"));
    }

    private Categoria categoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return catalogoRepository.buscarCategoria(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoria " + categoriaId));
    }

    private List<Habilidad> habilidades(List<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : catalogoRepository.buscarHabilidades(ids);
    }

    private PoliticaCancelacion politica(Long politicaId) {
        if (politicaId == null) {
            return null;
        }
        return politicaRepository.buscarPorId(politicaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la politica " + politicaId));
    }
}
